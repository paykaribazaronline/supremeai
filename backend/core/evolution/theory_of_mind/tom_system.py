"""
SupremeAI Theory of Mind System
===============================

Implements Theory of Mind capabilities allowing the AI system to:
- Understand beliefs, desires, and intentions of others
- Predict behavior based on mental states
- Model the knowledge and perspectives of other agents
- Reason about what others believe about the world and about itself

Theory of Mind (ToM) is crucial for advanced social intelligence and
collaborative AI systems.

Bengali:
মনের তত্ত্ব সিস্টেম
মনের তত্ত্বের ক্ষমতা বাস্তবায়ন করে যাতে এআই সিস্টেমটি:
- অন্যের বিশ্বাস, ইচ্ছা এবং উদ্দেশ্য বুঝতে পারে
- মানসিক অবস্থার উপর ভিত্তি করে আচরণ পূর্বাভাস দিতে পারে
- অন্য এজেন্টদের জ্ঞান এবং দৃষ্টিভঙ্গি মডেল করতে পারে
- অন্যের বিশ্বের সম্পর্কে কী বিশ্বাস করে এবং নিজের সম্পর্কে কী বিশ্বাস করে তা যৌক্তিক ভাবে চিন্তা করতে পারে
"""

from dataclasses import dataclass
from enum import Enum
from typing import Any

import numpy as np
import torch
import torch.nn as nn


class MentalStateType(Enum):
    BELIEF = "belief"
    DESIRE = "desire"
    INTENTION = "intention"
    KNOWLEDGE = "knowledge"
    EMOTION = "emotion"
    PERCEPTION = "perception"


class ToMLevel(Enum):
    """Theory of Mind sophistication levels."""

    LEVEL_0 = "level_0"  # No ToM - cannot attribute mental states
    LEVEL_1 = "level_1"  # Can attribute basic mental states to others
    LEVEL_2 = "level_2"  # Can understand that others may have different beliefs
    LEVEL_3 = "level_3"  # Can understand false beliefs and deception
    LEVEL_4 = "level_4"  # Can engage in recursive ToM (believing about believing)


@dataclass
class MentalState:
    """Represents a mental state of an agent."""

    agent_id: str
    state_type: MentalStateType
    content: str  # Description of the mental state
    confidence: float  # How confident the system is in this attribution
    timestamp: float
    source: str  # Where the information came from


@dataclass
class BeliefState:
    """Represents a belief held by an agent."""

    belief_id: str
    holder: str  # Agent holding the belief
    content: str  # What the agent believes
    is_true: bool | None  # Whether the belief is true (None if unknown)
    confidence: float  # Confidence in the belief attribution
    context: dict[str, Any]  # Context in which the belief exists


@dataclass
class DesireState:
    """Represents a desire or goal of an agent."""

    desire_id: str
    holder: str
    goal: str  # What the agent wants
    priority: float  # Priority level (0.0 to 1.0)
    feasibility: float  # How feasible the desire is (0.0 to 1.0)
    context: dict[str, Any]


@dataclass
class ToMConfig:
    """Configuration for Theory of Mind system."""

    # Model parameters
    embedding_dim: int = 256
    hidden_dim: int = 512
    num_layers: int = 2

    # Confidence thresholds
    belief_confidence_threshold: float = 0.7
    intention_confidence_threshold: float = 0.6

    # Memory parameters
    memory_size: int = 1000
    memory_decay_rate: float = 0.01

    # Reasoning parameters
    reasoning_depth: int = 3  # Maximum recursion depth for ToM reasoning
    attention_heads: int = 4


class SocialCognitionModule(nn.Module):
    """
    Neural module for social cognition and mental state attribution.
    """

    def __init__(self, config: ToMConfig):
        super().__init__()
        self.config = config

        # Embedding layers for different entity types
        self.agent_embedding = nn.Embedding(1000, config.embedding_dim)  # Agent IDs
        self.state_type_embedding = nn.Embedding(len(MentalStateType), config.embedding_dim)

        # Transformer-based architecture for reasoning
        encoder_layer = nn.TransformerEncoderLayer(
            d_model=config.embedding_dim,
            nhead=config.attention_heads,
            dim_feedforward=config.hidden_dim,
            dropout=0.1,
            batch_first=True,
        )
        self.transformer = nn.TransformerEncoder(encoder_layer, config.num_layers)

        # Output heads for different mental state types
        self.belief_predictor = nn.Linear(config.embedding_dim, 1)
        self.desire_predictor = nn.Linear(config.embedding_dim, 1)
        self.intention_predictor = nn.Linear(config.embedding_dim, 1)
        self.knowledge_predictor = nn.Linear(config.embedding_dim, 1)

        # Confidence estimation
        self.confidence_estimator = nn.Linear(config.embedding_dim, 1)

    def forward(
        self, agent_ids: torch.Tensor, state_types: torch.Tensor, context_embeddings: torch.Tensor
    ) -> dict[str, torch.Tensor]:
        """
        Forward pass for mental state attribution.

        Args:
            agent_ids: Tensor of agent identifiers
            state_types: Tensor of mental state types
            context_embeddings: Embeddings of context information

        Returns:
            Dictionary of predictions for different mental states
        """
        # Embed agent and state type information
        agent_embeds = self.agent_embedding(agent_ids)
        type_embeds = self.state_type_embedding(state_types)

        # Combine embeddings
        combined_embeds = torch.cat([agent_embeds.unsqueeze(1), type_embeds.unsqueeze(1), context_embeddings], dim=1)

        # Apply transformer for reasoning
        attended = self.transformer(combined_embeds)

        # Extract representations
        final_repr = attended[:, -1, :]  # Use last position for prediction

        # Make predictions
        outputs = {
            "belief": torch.sigmoid(self.belief_predictor(final_repr)),
            "desire": torch.sigmoid(self.desire_predictor(final_repr)),
            "intention": torch.sigmoid(self.intention_predictor(final_repr)),
            "knowledge": torch.sigmoid(self.knowledge_predictor(final_repr)),
            "confidence": torch.sigmoid(self.confidence_estimator(final_repr)),
        }

        return outputs


class MentalStateManager:
    """
    Manages mental states of multiple agents in the system.
    """

    def __init__(self, config: ToMConfig):
        self.config = config
        self.beliefs: dict[str, list[BeliefState]] = {}
        self.desires: dict[str, list[DesireState]] = {}
        self.intentions: dict[str, list[MentalState]] = {}
        self.knowledge: dict[str, list[MentalState]] = {}
        self.social_relations: dict[str, dict[str, float]] = {}  # Trust/confidence between agents

        # Initialize neural module
        self.neural_module = SocialCognitionModule(config)

    def update_belief(self, belief: BeliefState):
        """Update or add a belief for an agent."""
        if belief.holder not in self.beliefs:
            self.beliefs[belief.holder] = []

        # Remove old belief with same content if exists
        self.beliefs[belief.holder] = [b for b in self.beliefs[belief.holder] if b.content != belief.content]

        # Add new belief
        self.beliefs[belief.holder].append(belief)

    def update_desire(self, desire: DesireState):
        """Update or add a desire for an agent."""
        if desire.holder not in self.desires:
            self.desires[desire.holder] = []

        # Remove old desire with same goal if exists
        self.desires[desire.holder] = [d for d in self.desires[desire.holder] if d.goal != desire.goal]

        # Add new desire
        self.desires[desire.holder].append(desire)

    def get_agent_beliefs(self, agent_id: str) -> list[BeliefState]:
        """Get all beliefs attributed to an agent."""
        return self.beliefs.get(agent_id, [])

    def get_agent_desires(self, agent_id: str) -> list[DesireState]:
        """Get all desires attributed to an agent."""
        return self.desires.get(agent_id, [])

    def get_belief_about_world(self, agent_id: str, topic: str) -> BeliefState | None:
        """Get an agent's belief about a specific topic."""
        beliefs = self.get_agent_beliefs(agent_id)
        for belief in beliefs:
            if topic.lower() in belief.content.lower():
                return belief
        return None

    def get_shared_beliefs(self, agent1: str, agent2: str) -> list[BeliefState]:
        """Get beliefs that both agents share."""
        beliefs1 = set(b.content for b in self.get_agent_beliefs(agent1))
        beliefs2 = set(b.content for b in self.get_agent_beliefs(agent2))

        shared_contents = beliefs1.intersection(beliefs2)

        # Return beliefs from agent1 that are shared
        return [b for b in self.get_agent_beliefs(agent1) if b.content in shared_contents]

    def infer_mental_state(self, agent_observed: str, observed_behavior: str, context: dict[str, Any]) -> MentalState:
        """
        Infer a mental state from observed behavior and context.

        Args:
            agent_observed: Agent whose behavior was observed
            observed_behavior: Description of the behavior
            context: Contextual information

        Returns:
            Inferred mental state
        """
        # This is a simplified implementation
        # In a real system, this would use more sophisticated reasoning

        # Determine likely mental state type based on behavior
        if any(word in observed_behavior.lower() for word in ["want", "need", "desire", "wish"]):
            state_type = MentalStateType.DESIRE
        elif any(word in observed_behavior.lower() for word in ["think", "believe", "know"]):
            state_type = MentalStateType.BELIEF
        elif any(word in observed_behavior.lower() for word in ["go", "try", "attempt", "plan"]):
            state_type = MentalStateType.INTENTION
        else:
            state_type = MentalStateType.BELIEF  # Default assumption

        # Generate content based on behavior
        content = f"The agent {observed_behavior}"

        # Estimate confidence based on context richness
        confidence = min(0.9, 0.3 + len(context) * 0.1)

        return MentalState(
            agent_id=agent_observed,
            state_type=state_type,
            content=content,
            confidence=confidence,
            timestamp=context.get("timestamp", 0.0),
            source="behavior_inference",
        )


class FalseBeliefReasoner:
    """
    Handles reasoning about false beliefs - a key component of Theory of Mind.
    """

    def __init__(self):
        self.false_belief_scenarios = []

    def detect_false_belief(self, agent_id: str, believed_content: str, actual_content: str) -> tuple[bool, float]:
        """
        Detect if an agent holds a false belief.

        Args:
            agent_id: Agent to check
            believed_content: What the agent believes
            actual_content: What is actually true

        Returns:
            (is_false_belief, confidence)
        """
        # Compare the believed content with actual content
        # This is a simplified comparison - in reality would need more sophisticated NLU

        # Convert to lowercase for comparison
        believed_lower = believed_content.lower().strip()
        actual_lower = actual_content.lower().strip()

        # Check for contradictions
        if self._contradicts(believed_lower, actual_lower):
            return True, 0.9  # High confidence in contradiction

        # Check for inconsistencies
        if self._inconsistent(believed_lower, actual_lower):
            return True, 0.7  # Medium confidence in inconsistency

        # Check for outdated information
        if self._outdated(believed_lower, actual_lower):
            return True, 0.6  # Lower confidence in outdated info

        return False, 0.1  # Low confidence in true belief

    def _contradicts(self, believed: str, actual: str) -> bool:
        """Check if beliefs contradict each other."""
        # Look for explicit contradictions
        contradiction_keywords = [
            ("not", ""),
            ("never", "always"),
            ("no", "yes"),
            ("false", "true"),
            ("incorrect", "correct"),
        ]

        for neg_word, pos_word in contradiction_keywords:
            if neg_word in believed and pos_word in actual:
                return True
            if pos_word in believed and neg_word in actual:
                return True

        return False

    def _inconsistent(self, believed: str, actual: str) -> bool:
        """Check if beliefs are inconsistent."""
        # Check for incompatible claims about the same subject
        # This is a simplified check
        return False  # Placeholder implementation

    def _outdated(self, believed: str, actual: str) -> bool:
        """Check if belief is outdated."""
        # Check for temporal inconsistencies
        # This is a simplified check
        return False  # Placeholder implementation

    def generate_false_belief_scenario(self, agent_a: str, agent_b: str, object_location: str) -> dict[str, Any]:
        """
        Generate a classic false belief scenario (Sally-Anne test variant).

        Args:
            agent_a: First agent (e.g., Sally)
            agent_b: Second agent (e.g., Anne)
            object_location: Initial location of object

        Returns:
            Scenario description and expected reasoning
        """
        scenario = {
            "agents": [agent_a, agent_b],
            "initial_state": {
                "object_location": object_location,
                "who_knows_location": [agent_a],  # Only agent_a knows initial location
            },
            "intervention": {"actor": agent_b, "action": "moved_object", "new_location": "different_location"},
            "post_intervention_state": {
                "actual_location": "different_location",
                "agent_a_believes_location": object_location,  # Still believes old location
                "agent_b_knows_location": "different_location",
            },
            "question": f"Where does {agent_a} think the object is?",
            "correct_answer": object_location,
            "expected_reasoning": f"{agent_a} should falsely believe the object is still at {object_location} because {agent_a} did not witness the move.",
        }

        return scenario


class ToMReasoner:
    """
    Performs Theory of Mind reasoning and perspective taking.
    """

    def __init__(self, config: ToMConfig):
        self.config = config
        self.mental_state_manager = MentalStateManager(config)
        self.false_belief_reasoner = FalseBeliefReasoner()
        self.reasoning_cache = {}

    def reason_about_beliefs(self, agent_id: str, target_topic: str) -> dict[str, Any]:
        """
        Reason about what an agent believes regarding a specific topic.

        Args:
            agent_id: Agent whose beliefs are being analyzed
            target_topic: Topic of interest

        Returns:
            Analysis of the agent's beliefs about the topic
        """
        belief = self.mental_state_manager.get_belief_about_world(agent_id, target_topic)

        if not belief:
            return {"agent": agent_id, "topic": target_topic, "has_belief": False, "confidence": 0.0}

        # Check if the belief might be false
        is_false, false_confidence = self.false_belief_reasoner.detect_false_belief(
            agent_id, belief.content, f"actual truth about {target_topic}"
        )

        return {
            "agent": agent_id,
            "topic": target_topic,
            "has_belief": True,
            "belief_content": belief.content,
            "belief_is_true": not is_false,
            "false_belief_confidence": false_confidence,
            "belief_confidence": belief.confidence,
        }

    def predict_behavior(self, agent_id: str, situation: str) -> dict[str, Any]:
        """
        Predict how an agent will behave in a given situation based on their mental states.

        Args:
            agent_id: Agent whose behavior is being predicted
            situation: Description of the situation

        Returns:
            Prediction of likely behavior with confidence
        """
        # Get agent's beliefs and desires relevant to the situation
        beliefs = self.mental_state_manager.get_agent_beliefs(agent_id)
        desires = self.mental_state_manager.get_agent_desires(agent_id)

        # Filter for situation-relevant mental states
        relevant_beliefs = [b for b in beliefs if any(word in b.content.lower() for word in situation.lower().split())]

        relevant_desires = [d for d in desires if any(word in d.goal.lower() for word in situation.lower().split())]

        # Generate prediction based on goal-directed behavior
        if relevant_desires:
            # Agent will likely act to fulfill most highly prioritized relevant desire
            top_desire = max(relevant_desires, key=lambda d: d.priority)

            # Consider beliefs about feasibility and context
            feasible_desires = [d for d in relevant_desires if d.feasibility > 0.5]

            if feasible_desires:
                prediction = f"The agent will attempt to achieve: {feasible_desires[0].goal}"
                confidence = feasible_desires[0].priority * feasible_desires[0].feasibility
            else:
                prediction = f"The agent recognizes desire to achieve: {top_desire.goal} but perceives it as infeasible"
                confidence = top_desire.priority * (1 - top_desire.feasibility)
        else:
            # No strong desires, behavior may be based on beliefs alone
            if relevant_beliefs:
                prediction = f"The agent will act based on belief: {relevant_beliefs[0].content}"
                confidence = relevant_beliefs[0].confidence
            else:
                prediction = "Insufficient mental state information to predict behavior"
                confidence = 0.1

        return {
            "predicted_behavior": prediction,
            "confidence": confidence,
            "relevant_beliefs": [b.content for b in relevant_beliefs],
            "relevant_desires": [d.goal for d in relevant_desires],
        }

    def perspective_taking(self, observer: str, target: str, situation: str) -> dict[str, Any]:
        """
        Take the perspective of one agent observing another in a situation.

        Args:
            observer: Agent doing the observing
            target: Agent being observed
            situation: Situation context

        Returns:
            Observer's perspective on target's mental states and likely behavior
        """
        # Get observer's beliefs about the target
        observer_beliefs_about_target = [
            b for b in self.mental_state_manager.get_agent_beliefs(observer) if target in b.content
        ]

        # Get target's actual mental states
        target_beliefs = self.mental_state_manager.get_agent_beliefs(target)
        target_desires = self.mental_state_manager.get_agent_desires(target)

        # Analyze discrepancy between observer's beliefs and target's actual states
        belief_alignment = len(
            [b for b in observer_beliefs_about_target if any(tb.content in b.content for tb in target_beliefs)]
        ) / max(1, len(observer_beliefs_about_target))

        # Predict what observer thinks target will do
        predicted_target_behavior = self.predict_behavior(target, situation)

        # Predict what observer thinks target believes
        target_belief_prediction = self.reason_about_beliefs(target, situation)

        return {
            "observer": observer,
            "target": target,
            "situation": situation,
            "observer_beliefs_about_target": [b.content for b in observer_beliefs_about_target],
            "target_actual_beliefs": [b.content for b in target_beliefs],
            "target_actual_desires": [d.goal for d in target_desires],
            "belief_alignment": belief_alignment,
            "observer_prediction_of_target_behavior": predicted_target_behavior,
            "observer_prediction_of_target_beliefs": target_belief_prediction,
        }

    def recursive_reasoning(self, agent_a: str, agent_b: str, depth: int = 1) -> dict[str, Any]:
        """
        Perform recursive Theory of Mind reasoning (A believes that B believes that...).

        Args:
            agent_a: First agent
            agent_b: Second agent
            depth: Depth of recursion

        Returns:
            Recursive reasoning results
        """
        if depth > self.config.reasoning_depth or depth <= 0:
            return {"error": "Recursion depth exceeded or invalid"}

        # Base case: A's belief about B's belief
        if depth == 1:
            # Get A's beliefs about B
            a_beliefs_about_b = [
                b for b in self.mental_state_manager.get_agent_beliefs(agent_a) if agent_b in b.content
            ]

            # Get B's actual beliefs
            b_beliefs = self.mental_state_manager.get_agent_beliefs(agent_b)

            return {
                "depth": 1,
                "agent_a": agent_a,
                "agent_b": agent_b,
                "a_beliefs_about_b": [b.content for b in a_beliefs_about_b],
                "b_actual_beliefs": [b.content for b in b_beliefs],
                "alignment": len([ab for ab in a_beliefs_about_b if any(bb.content in ab.content for bb in b_beliefs)]),
            }

        # Recursive case: A believes that B believes that...
        else:
            # Get the reasoning at depth - 1
            prev_reasoning = self.recursive_reasoning(agent_a, agent_b, depth - 1)

            # Now consider what agent_a believes about agent_b's beliefs at the previous level
            # This is a simplified implementation - full recursive ToM is very complex
            return {
                "depth": depth,
                "recursive_step": prev_reasoning,
                "note": "Full recursive ToM implementation requires more sophisticated modeling",
            }

    def assess_tom_level(self, agent_id: str) -> ToMLevel:
        """
        Assess the Theory of Mind sophistication level of an agent.

        Args:
            agent_id: Agent to assess

        Returns:
            Estimated ToM level
        """
        beliefs = self.mental_state_manager.get_agent_beliefs(agent_id)
        desires = self.mental_state_manager.get_agent_desires(agent_id)

        # Level 0: No mental state attribution
        if not beliefs and not desires:
            return ToMLevel.LEVEL_0

        # Level 1: Basic mental state attribution
        if len(beliefs) > 0 or len(desires) > 0:
            # Check if agent attributes mental states to others
            other_mention_count = sum(
                1 for b in beliefs if "other" in b.content.lower() or "another" in b.content.lower()
            )
            if other_mention_count > 0:
                # Level 2: Understanding different beliefs
                different_beliefs = any(b.is_true is False for b in beliefs if hasattr(b, "is_true"))
                if different_beliefs:
                    # Level 3: Understanding false beliefs
                    false_belief_scenarios = self.false_belief_reasoner.false_belief_scenarios
                    if len(false_belief_scenarios) > 0:
                        # Level 4: Recursive ToM
                        # This would require evidence of recursive reasoning
                        return ToMLevel.LEVEL_3  # For now, max at level 3 in this implementation
                    return ToMLevel.LEVEL_3
                return ToMLevel.LEVEL_2
            return ToMLevel.LEVEL_1

        return ToMLevel.LEVEL_0


class TheoryOfMindSystem:
    """
    Main Theory of Mind system integrating all components.
    """

    def __init__(self, config: ToMConfig = None):
        self.config = config or ToMConfig()
        self.reasoner = ToMReasoner(self.config)
        self.conversation_history = []

    def process_interaction(self, speaker: str, message: str, context: dict[str, Any] | None = None) -> dict[str, Any]:
        """
        Process an interaction to update mental state attributions.

        Args:
            speaker: Agent who spoke
            message: What was said
            context: Additional context

        Returns:
            Analysis of the interaction and updated beliefs
        """
        context = context or {}

        # Infer mental states from the message
        inferred_state = self.reasoner.mental_state_manager.infer_mental_state(
            agent_observed=speaker, observed_behavior=message, context=context
        )

        # Store the interaction
        interaction_record = {
            "speaker": speaker,
            "message": message,
            "inferred_state": {
                "type": inferred_state.state_type.value,
                "content": inferred_state.content,
                "confidence": inferred_state.confidence,
            },
            "timestamp": inferred_state.timestamp,
        }

        self.conversation_history.append(interaction_record)

        # Update mental state manager if it's a belief
        if inferred_state.state_type == MentalStateType.BELIEF:
            belief_state = BeliefState(
                belief_id=f"belief_{len(self.conversation_history)}",
                holder=speaker,
                content=inferred_state.content,
                is_true=None,  # Unknown initially
                confidence=inferred_state.confidence,
                context=context,
            )
            self.reasoner.mental_state_manager.update_belief(belief_state)

        # Perform analysis
        analysis = {
            "interaction_processed": interaction_record,
            "speaker_tom_level": self.reasoner.assess_tom_level(speaker),
            "related_beliefs": [b.content for b in self.reasoner.mental_state_manager.get_agent_beliefs(speaker)],
            "predictions": self.reasoner.predict_behavior(speaker, message),
        }

        return analysis

    def analyze_social_dynamics(self, agents: list[str]) -> dict[str, Any]:
        """
        Analyze the social dynamics and Theory of Mind relationships between agents.

        Args:
            agents: List of agent IDs to analyze

        Returns:
            Analysis of social dynamics
        """
        dynamics = {
            "agents_analyzed": agents,
            "individual_tom_levels": {},
            "pairwise_relationships": {},
            "group_dynamics": {},
        }

        # Analyze individual ToM levels
        for agent in agents:
            dynamics["individual_tom_levels"][agent] = self.reasoner.assess_tom_level(agent).value

        # Analyze pairwise relationships
        for i, agent_a in enumerate(agents):
            for j, agent_b in enumerate(agents):
                if i != j:
                    relationship_key = f"{agent_a}_to_{agent_b}"
                    perspective = self.reasoner.perspective_taking(agent_a, agent_b, "general interaction")
                    dynamics["pairwise_relationships"][relationship_key] = perspective

        # Analyze group dynamics
        all_beliefs = []
        all_desires = []

        for agent in agents:
            all_beliefs.extend(self.reasoner.mental_state_manager.get_agent_beliefs(agent))
            all_desires.extend(self.reasoner.mental_state_manager.get_agent_desires(agent))

        # Find shared beliefs and conflicting desires

        # Simplified analysis
        belief_contents = [b.content for b in all_beliefs]
        desire_goals = [d.goal for d in all_desires]

        # Group dynamics summary
        dynamics["group_dynamics"] = {
            "total_beliefs": len(all_beliefs),
            "total_desires": len(all_desires),
            "unique_beliefs": len(set(belief_contents)),
            "unique_desires": len(set(desire_goals)),
            "average_belief_confidence": np.mean([b.confidence for b in all_beliefs]) if all_beliefs else 0.0,
        }

        return dynamics

    def generate_insight(self, agent_id: str, focus_area: str = "beliefs") -> str:
        """
        Generate insights about an agent's mental states.

        Args:
            agent_id: Agent to analyze
            focus_area: Area of focus ('beliefs', 'desires', 'behavior', 'relationships')

        Returns:
            Generated insight
        """
        if focus_area == "beliefs":
            beliefs = self.reasoner.mental_state_manager.get_agent_beliefs(agent_id)
            if beliefs:
                most_confident = max(beliefs, key=lambda b: b.confidence)
                return f"Agent {agent_id} strongly believes that '{most_confident.content}' with confidence {most_confident.confidence:.2f}."
            else:
                return f"Agent {agent_id} has no attributed beliefs."

        elif focus_area == "desires":
            desires = self.reasoner.mental_state_manager.get_agent_desires(agent_id)
            if desires:
                highest_priority = max(desires, key=lambda d: d.priority)
                return f"Agent {agent_id}'s highest priority desire is to '{highest_priority.goal}' with priority {highest_priority.priority:.2f}."
            else:
                return f"Agent {agent_id} has no attributed desires."

        elif focus_area == "behavior":
            # Predict likely behavior
            prediction = self.reasoner.predict_behavior(agent_id, "general context")
            return f"Agent {agent_id} is predicted to {prediction['predicted_behavior']} with confidence {prediction['confidence']:.2f}."

        elif focus_area == "relationships":
            # Analyze relationships with other agents
            other_agents = [aid for aid in self.reasoner.mental_state_manager.beliefs.keys() if aid != agent_id]
            if other_agents:
                other = other_agents[0]  # Pick first other agent
                perspective = self.reasoner.perspective_taking(agent_id, other, "general interaction")
                alignment = perspective["belief_alignment"]
                return f"Agent {agent_id} has {alignment:.2f} alignment in beliefs with {other}."
            else:
                return f"Agent {agent_id} has no other agents to relate to in the system."

        else:
            return f"Unknown focus area: {focus_area}"


# Example usage and testing
def demo_theory_of_mind():
    """Demonstrate Theory of Mind system capabilities."""
    print("Initializing Theory of Mind System...")

    # Create system
    config = ToMConfig()
    tom_system = TheoryOfMindSystem(config)

    # Simulate interactions
    interactions = [
        ("Alice", "I believe the meeting is at 3 PM", {"context": "office", "topic": "meeting"}),
        ("Bob", "I want to finish this project today", {"context": "work", "topic": "project"}),
        ("Charlie", "Alice said the meeting is at 2 PM", {"context": "conversation", "topic": "meeting"}),
    ]

    print("\nProcessing interactions...")
    for speaker, message, context in interactions:
        result = tom_system.process_interaction(speaker, message, context)
        print(f"Processed: {speaker} said '{message}'")
        print(f"  ToM Level: {result['speaker_tom_level'].value}")
        print(f"  Predicted Behavior: {result['predictions']['predicted_behavior']}")
        print()

    # Analyze social dynamics
    print("Analyzing social dynamics...")
    agents = ["Alice", "Bob", "Charlie"]
    dynamics = tom_system.analyze_social_dynamics(agents)

    print(f"Individual ToM levels: {dynamics['individual_tom_levels']}")
    print(f"Group dynamics: {dynamics['group_dynamics']}")

    # Generate insights
    print("\nGenerating insights...")
    for agent in agents:
        insight = tom_system.generate_insight(agent, "beliefs")
        print(f"  {agent} belief insight: {insight}")

    # Test false belief reasoning
    print("\nTesting false belief reasoning...")
    false_belief_reasoner = FalseBeliefReasoner()
    scenario = false_belief_reasoner.generate_false_belief_scenario("Alice", "Bob", "drawer")
    print(f"False belief scenario: {scenario['question']}")
    print(f"Expected answer: {scenario['correct_answer']}")

    # Test recursive reasoning
    print("\nTesting recursive reasoning...")
    recursive_result = tom_system.reasoner.recursive_reasoning("Alice", "Bob", depth=1)
    print(f"Recursive reasoning result: {recursive_result}")


if __name__ == "__main__":
    demo_theory_of_mind()
