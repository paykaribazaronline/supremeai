from __future__ import annotations

# Phase 3 Self-Evolution Layer Exports
from evolution.advanced_evolution_engine import AdvancedEvolutionEngine, EvolutionMode, FitnessLandscape
from evolution.auto_evolution_controller import (
    AutoEvolutionController,
    EvolutionCycle,
    EvolutionPriority,
    EvolutionState,
    EvolutionTrigger,
    SystemHealth,
)
from evolution.auto_tuner import AutoTuner, TuningParameter, TuningResult, TuningStrategy
from evolution.memory_consolidator import (
    ConsolidationAction,
    ConsolidationResult,
    MemoryBlock,
    MemoryConsolidator,
    MemoryTier,
)
from evolution.performance_monitor import (
    AlertRule,
    AlertSeverity,
    AnomalyDetector,
    MetricPoint,
    MetricType,
    PerformanceAlert,
    PerformanceMonitor,
    PerformanceReport,
    PerformanceSnapshot,
)
from evolution.strategy_optimizer import (
    Strategy,
    StrategyOptimizer,
    StrategyStatus,
    StrategyType,
)

# Re-export from core.evolution
from core.evolution import (
    AgentBreeder,
    AutoSkillCreator,
    DailyLearner,
    EvolutionEngine,
    EvolutionSkillGraph,
    FitnessEngine,
    PerformanceOracle,
    SelfEvolutionAgent,
    SelfUpdater,
)

__all__ = [
    # Phase 3 Exports
    "AdvancedEvolutionEngine",
    "EvolutionMode",
    "FitnessLandscape",
    "AutoEvolutionController",
    "EvolutionCycle",
    "EvolutionPriority",
    "EvolutionState",
    "EvolutionTrigger",
    "SystemHealth",
    "AutoTuner",
    "TuningParameter",
    "TuningResult",
    "TuningStrategy",
    "ConsolidationAction",
    "ConsolidationResult",
    "MemoryBlock",
    "MemoryConsolidator",
    "MemoryTier",
    "AlertRule",
    "AlertSeverity",
    "AnomalyDetector",
    "MetricPoint",
    "MetricType",
    "PerformanceAlert",
    "PerformanceMonitor",
    "PerformanceReport",
    "PerformanceSnapshot",
    "Strategy",
    "StrategyOptimizer",
    "StrategyStatus",
    "StrategyType",
    # Core Evolution Exports
    "AgentBreeder",
    "AutoSkillCreator",
    "DailyLearner",
    "EvolutionEngine",
    "EvolutionSkillGraph",
    "FitnessEngine",
    "PerformanceOracle",
    "SelfEvolutionAgent",
    "SelfUpdater",
]
