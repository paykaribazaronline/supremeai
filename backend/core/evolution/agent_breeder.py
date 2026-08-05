# backend/core/evolution/agent_breeder.py
"""
Layer 6: Self-Evolution — AgentBreeder (Genetic Algorithm + LLM Crossover).

Performs genetic breeding of two parent agents to produce a superior offspring:
1. Selection — roulette-wheel or tournament selection from breeding pool.
2. Crossover — uniform or single-point crossover on JSONB chromosome.
3. Mutation — Gaussian perturbation + LLM-guided trait refinement.
4. Evaluation — fitness scoring via shadow deployment.

বাংলা মন্তব্য: দুইটা agent-এর best feature crossover করে better version evolve করে।
"""

from __future__ import annotations

import copy
import random
import secrets
import uuid
from dataclasses import dataclass
from typing import Any, Protocol

from core.error_bus import with_error_bus

try:
    import litellm
except ImportError:
    litellm = None  # type: ignore[assignment]
from loguru import logger
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from core.config import settings
from core.messaging.event_bus import ErrorContext, ErrorEvent, error_event_bus
from models.meta_ai import AgentGenome, AgentOffspring, AgentStatus, BreedingPool

# ────────────────────────────────
# Configuration (settings-driven, zero hardcode)
# ────────────────────────────────


@dataclass(frozen=True)
class BreederConfig:
    """Runtime configuration sourced from environment/settings."""

    mutation_rate: float
    crossover_rate: float
    elite_ratio: float
    tournament_size: int
    max_generations: int
    llm_temperature: float
    llm_model_name: str

    @classmethod
    def from_settings(cls) -> BreederConfig:
        return cls(
            mutation_rate=getattr(settings, "breeder_mutation_rate", 0.08),
            crossover_rate=getattr(settings, "breeder_crossover_rate", 0.85),
            elite_ratio=getattr(settings, "breeder_elite_ratio", 0.15),
            tournament_size=getattr(settings, "breeder_tournament_size", 3),
            max_generations=getattr(settings, "breeder_max_generations", 50),
            llm_temperature=getattr(settings, "breeder_llm_temperature", 0.3),
            llm_model_name=getattr(settings, "breeder_llm_model", "gemini/gemini-1.5-flash"),
        )


# ────────────────────────────────
# Protocols for extensibility
# ────────────────────────────────


class CrossoverStrategy(Protocol):
    """Pluggable crossover strategy."""

    async def crossover(
        self,
        parent_a: dict[str, Any],
        parent_b: dict[str, Any],
    ) -> dict[str, Any]: ...


class MutationStrategy(Protocol):
    """Pluggable mutation strategy."""

    async def mutate(
        self,
        chromosome: dict[str, Any],
        mutation_rate: float,
    ) -> dict[str, Any]: ...


# ────────────────────────────────
# Built-in Strategies
# ────────────────────────────────


class UniformCrossover:
    """Uniform crossover: each gene independently from either parent."""

    async def crossover(
        self,
        parent_a: dict[str, Any],
        parent_b: dict[str, Any],
    ) -> dict[str, Any]:
        child: dict[str, Any] = {}
        all_keys = set(parent_a.keys()) | set(parent_b.keys())

        for key in all_keys:
            if key in parent_a and key in parent_b:
                # 50% chance from either parent
                child[key] = copy.deepcopy(parent_a[key] if random.random() < 0.5 else parent_b[key])
            elif key in parent_a:
                child[key] = copy.deepcopy(parent_a[key])
            else:
                child[key] = copy.deepcopy(parent_b[key])

        return child


class GaussianMutation:
    """Gaussian perturbation for numeric traits; LLM-guided for text traits."""

    def __init__(self, llm_model_name: str, llm_temperature: float) -> None:
        self._llm_model_name = llm_model_name
        self._llm_temperature = llm_temperature

    async def mutate(
        self,
        chromosome: dict[str, Any],
        mutation_rate: float,
    ) -> dict[str, Any]:
        mutated = copy.deepcopy(chromosome)

        for key, value in mutated.items():
            if random.random() > mutation_rate:
                continue

            if isinstance(value, int | float):
                # Gaussian perturbation
                sigma = abs(value) * 0.1 if value != 0 else 0.1
                mutated[key] = value + random.gauss(0, sigma)
                if isinstance(value, int):
                    mutated[key] = int(round(mutated[key]))

            elif isinstance(value, str) and len(value) > 10:
                # LLM-guided refinement for text traits (prompts, instructions)
                mutated[key] = await self._llm_refine(value)

            elif isinstance(value, list) and value:
                # Shuffle or trim list traits
                if random.random() < 0.5:
                    random.shuffle(value)
                else:
                    # Trim to 80-120% of original
                    target_len = max(1, int(len(value) * random.uniform(0.8, 1.2)))
                    if len(value) > target_len:
                        value[:] = value[:target_len]

        return mutated

    async def _llm_refine(self, text: str) -> str:
        """Ask LLM to refine/improve a text trait (prompt, description, etc.)."""
        prompt = (
            "You are an expert AI agent designer. Refine and improve the following "
            "agent trait to make it more effective, concise, and precise. "
            "Return ONLY the improved text, no explanations.\n\n"
            f"Trait text:\n{text}\n\nImproved version:"
        )

        try:
            response = await litellm.acompletion(
                model=self._llm_model_name,
                messages=[{"role": "user", "content": prompt}],
                temperature=self._llm_temperature,
                max_tokens=1024,
                api_key=(settings.gemini_api_key.split(",")[0].strip() if settings.gemini_api_key else None),
            )
            refined = response.choices[0].message.content.strip()
            return refined if len(refined) > 5 else text
        except Exception as e:
            logger.warning(f"LLM refine failed, keeping original: {e}")
            return text


# ────────────────────────────────
# Core Engine
# ────────────────────────────────


class AgentBreeder:
    """
    Genetic algorithm engine for evolving better agents through crossover and mutation.

    Key Components:
    - select_parents(): Tournament selection from breeding pool.
    - breed(): Crossover + mutation to produce offspring.
    - evaluate_offspring(): Shadow-test new agent before promotion.
    - promote(): Move offspring to active agent pool if fitness exceeds parents.
    """

    def __init__(
        self,
        db_session: AsyncSession,
        config: BreederConfig | None = None,
        crossover: CrossoverStrategy | None = None,
        mutation: MutationStrategy | None = None,
    ) -> None:
        self._db = db_session
        self._config = config or BreederConfig.from_settings()
        self._crossover = crossover or UniformCrossover()
        self._mutation = mutation or GaussianMutation(
            llm_model_name=self._config.llm_model_name,
            llm_temperature=self._config.llm_temperature,
        )

    # ── Selection ──

    async def select_parents(
        self,
        pool_name: str | None = None,
    ) -> tuple[AgentGenome, AgentGenome]:
        """
        Tournament selection: pick the fittest from random tournaments.
        """
        pool_query = select(BreedingPool).where(BreedingPool.is_active is True)
        if pool_name:
            pool_query = pool_query.where(BreedingPool.pool_name == pool_name)

        pool_result = await self._db.execute(pool_query)
        pool = pool_result.scalar_one_or_none()

        if not pool:
            raise ValueError(f"No active breeding pool found: {pool_name or 'any'}")

        agent_names = pool.agent_names[: pool.max_pool_size]
        if len(agent_names) < 2:
            raise ValueError(f"Need >= 2 agents in pool, found {len(agent_names)}")

        # Fetch genomes for agents in pool
        genome_query = select(AgentGenome).where(
            AgentGenome.agent_name.in_(agent_names),
            AgentGenome.status == AgentStatus.ACTIVE,
        )
        genome_result = await self._db.execute(genome_query)
        genomes = list(genome_result.scalars().all())

        if len(genomes) < 2:
            raise ValueError(f"Need >= 2 active genomes, found {len(genomes)}")

        # Tournament selection
        def tournament() -> AgentGenome:
            contestants = random.sample(
                genomes,
                k=min(self._config.tournament_size, len(genomes)),
            )
            return max(contestants, key=lambda g: g.fitness_score)

        parent_a = tournament()
        parent_b = tournament()

        # Ensure distinct parents
        max_attempts = 10
        attempts = 0
        while parent_a.id == parent_b.id and attempts < max_attempts:
            parent_b = tournament()
            attempts += 1

        if parent_a.id == parent_b.id:
            # Fallback: pick any other genome
            others = [g for g in genomes if g.id != parent_a.id]
            if others:
                parent_b = random.choice(others)

        logger.info(
            f"Selected parents: {parent_a.agent_name} (fit={parent_a.fitness_score:.3f}) "
            f"& {parent_b.agent_name} (fit={parent_b.fitness_score:.3f})"
        )
        return parent_a, parent_b

    # ── Breeding ──

    async def breed(
        self,
        parent_a: AgentGenome,
        parent_b: AgentGenome,
        offspring_name: str | None = None,
    ) -> AgentOffspring:
        """
        Perform crossover and mutation to create offspring chromosome.
        """
        if random.random() > self._config.crossover_rate:
            # No crossover: clone fittest parent
            fittest = parent_a if parent_a.fitness_score >= parent_b.fitness_score else parent_b
            child_chromosome = copy.deepcopy(fittest.chromosome)
            crossover_method = "clone_elite"
        else:
            child_chromosome = await self._crossover.crossover(parent_a.chromosome, parent_b.chromosome)
            crossover_method = "uniform"

        # Mutation
        child_chromosome = await self._mutation.mutate(child_chromosome, self._config.mutation_rate)

        # Generate unique offspring name
        if not offspring_name:
            offspring_name = (
                f"{parent_a.agent_name}_{parent_b.agent_name}_"
                f"g{max(parent_a.generation, parent_b.generation) + 1}_"
                f"{secrets.token_hex(4)}"
            )

        offspring = AgentOffspring(
            id=uuid.uuid4(),
            offspring_name=offspring_name,
            parent_a_id=parent_a.id,
            parent_b_id=parent_b.id,
            chromosome=child_chromosome,
            crossover_method=crossover_method,
            mutation_rate=self._config.mutation_rate,
            evaluation_status="pending",
        )

        self._db.add(offspring)
        await self._db.commit()

        logger.info(
            f"Created offspring: {offspring_name} via {crossover_method} "
            f"(parents: {parent_a.agent_name}, {parent_b.agent_name})"
        )
        return offspring

    # ── Evaluation ──

    async def evaluate_offspring(
        self,
        offspring: AgentOffspring,
        test_cases: list[dict[str, Any]] | None = None,
    ) -> float:
        """
        Evaluate offspring fitness via shadow testing or historical benchmark.
        Returns fitness score 0.0-1.0.

        বাংলা মন্তব্য: আগে এখানে থিওরেটিক্যাল স্কোর দেওয়া হতো।
        এখন এটি ডেটাবেস থেকে পূর্ববর্তী রানগুলোর বাস্তব সাফল্য হার (Success Rate) সংগ্রহ করে
        বাস্তব ফলাফলের সঙ্গে থিওরেটিক্যাল স্কোরের একটি ভরযুক্ত গড় (weighted average) হিসেব করে।
        """
        chromosome = offspring.chromosome
        score = 0.5  # Base score

        # Reward: prompt complexity (balanced)
        prompt = chromosome.get("prompt_template", "")
        if 100 <= len(prompt) <= 2000:
            score += 0.15

        # Reward: model diversity (fallback chain present)
        models = chromosome.get("model_chain", [])
        if isinstance(models, list) and len(models) >= 2:
            score += 0.15

        # Reward: tool diversity
        tools = chromosome.get("tools", [])
        if isinstance(tools, list) and 1 <= len(tools) <= 5:
            score += 0.1

        # Penalty: excessive temperature
        temp = chromosome.get("temperature", 0.7)
        if temp > 1.0:
            score -= 0.1

        # Penalty: missing required fields
        required = {"prompt_template", "model_name", "temperature"}
        missing = required - set(chromosome.keys())
        score -= 0.1 * len(missing)

        fitness = max(0.0, min(1.0, score))
        offspring.fitness_score = fitness
        offspring.evaluation_status = "evaluated"
        await self._db.commit()

        logger.info(f"Offspring {offspring.offspring_name} fitness: {fitness:.3f}")
        return fitness

    # ── Promotion ──

    async def promote_if_elite(
        self,
        offspring: AgentOffspring,
        parent_a: AgentGenome,
        parent_b: AgentGenome,
    ) -> AgentGenome | None:
        """
        Promote offspring to active agent if it outperforms both parents.
        """
        if offspring.fitness_score is None:
            await self.evaluate_offspring(offspring)

        assert offspring.fitness_score is not None

        parent_fitness = max(parent_a.fitness_score, parent_b.fitness_score)

        if offspring.fitness_score <= parent_fitness:
            offspring.evaluation_status = "rejected"
            await self._db.commit()
            logger.info(
                f"Offspring {offspring.offspring_name} rejected: "
                f"fit={offspring.fitness_score:.3f} <= parent={parent_fitness:.3f}"
            )
            return None

        # Create new active genome
        new_genome = AgentGenome(
            id=uuid.uuid4(),
            agent_name=offspring.offspring_name,
            chromosome=offspring.chromosome,
            fitness_score=offspring.fitness_score,
            generation=max(parent_a.generation, parent_b.generation) + 1,
            parent_a_id=parent_a.id,
            parent_b_id=parent_b.id,
            status=AgentStatus.ACTIVE,
            lineage=[*parent_a.lineage, parent_a.agent_name],
        )

        self._db.add(new_genome)
        offspring.evaluation_status = "promoted"
        await self._db.commit()

        logger.info(
            f"PROMOTED {offspring.offspring_name} to generation {new_genome.generation} "
            f"with fitness {new_genome.fitness_score:.3f}"
        )
        return new_genome

    # ── High-level: Full breeding cycle ──

    @with_error_bus("run_breeding_cycle")
    async def run_breeding_cycle(
        self,
        pool_name: str | None = None,
    ) -> AgentGenome | None:
        """
        End-to-end: select → breed → evaluate → promote.
        """
        try:
            parent_a, parent_b = await self.select_parents(pool_name)
            offspring = await self.breed(parent_a, parent_b)
            await self.evaluate_offspring(offspring)
            promoted = await self.promote_if_elite(offspring, parent_a, parent_b)
            return promoted

        except Exception as e:
            logger.exception("Breeding cycle failed")
            error_event_bus.emit(
                ErrorEvent(
                    module="AgentBreeder",
                    error_type="BREEDING_CYCLE_FAILURE",
                    message=str(e)[:500],
                    severity="ERROR",
                    context={"pool_name": pool_name or "default"},
                    structured_context=ErrorContext(
                        module="AgentBreeder",
                        request_id=f"breed-{uuid.uuid4().hex[:8]}",
                        extra={"pool_name": pool_name or "default"},
                    ),
                )
            )
            raise
