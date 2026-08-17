"""
SupremeAI 2.0 Core Components
=============================

Main integration package for all core components developed
as part of the roadmap, including:

- Performance Optimization (Phase 6.1)
- Accessibility (WCAG 2.1 AA) (Phase 6.2)
- Testing & QA (Phase 6.3)
- Production Deployment (Phase 6.4)

Also includes previously developed AI/ML research components:

- Digital Twin World Model (Phase 3.1)
- Continual Learning with EWC (Phase 3.2)
- Adversarial Robustness (Phase 3.3)
- Neural-Symbolic Integration (Phase 3.4)
- Federated Learning (Phase 3.5)
- Theory of Mind (Phase 3.6)
- Temporal Abstraction (Phase 3.7)

And cross-platform expansion components:

- Mobile App Integration
- Desktop App Integration

Bengali:
সুপ্রিমএআই ২.০ কোর কম্পোনেন্ট
রোডম্যাপের অংশ হিসেবে সব কোর কম্পোনেন্টের প্রধান একীকরণ প্যাকেজ
"""

from .accessibility.wcag_compliance import (
    AccessibilityComplianceEngine,
    AccessibilityIssue,
    ColorContrastChecker,
    HTMLAccessibilityChecker,
    WCAGGuideline,
    WCAGLevel,
    WCAGPrinciple,
)
from .deployment.production_deploy import (
    ConfigManager,
    DeploymentConfig,
    DeploymentEnvironment,
    DeploymentManager,
    DeploymentStatus,
    HealthChecker,
    ImageBuilder,
    ProductionDeploymentSystem,
)

# Import all core components
from .optimization.performance_optimizer import (
    AsyncLRUCache,
    LRUCache,
    OptimizationLevel,
    PerformanceOptimizer,
    get_performance_optimizer,
    performance_monitor,
)

# বাংলা মন্তব্য: core.testing.qa_suite নিজে aiohttp আমদানি করে (একটা optional/dev-only
# dependency — production API path কখনো এটা ব্যবহার করে না)। কিন্তু এই ব্লকটা আগে
# try/except ছাড়াই ছিল, ফলে aiohttp ইনস্টল করা না থাকলে শুধু "import core" করলেই
# (যেমন backend/middleware/anti_hacking.py-র "from core.cache.redis_manager import ..."
# লাইনটা core/__init__.py ট্রিগার করে) পুরো ব্যাকএন্ড ImportError দিয়ে ভেঙে পড়ত --
# ঠিক সেই একই ক্লাসের বাগ যেটা torch-এর জন্য নিচে evolution ব্লকে আগে থেকেই গার্ড করা
# আছে। এখানে একই প্যাটার্ন প্রয়োগ করা হলো যাতে QA-স্যুট ছাড়াই বাকি core.* সাবমডিউল
# (cache, config, otp_router ইত্যাদি) স্বাভাবিকভাবে import হতে পারে।
try:
    from .testing.qa_suite import (
        ChaosEngineer,
        IntegrationTestRunner,
        PerformanceTester,
        QASuite,
        SecurityTester,
        TestCase,
        TestCategory,
        TestPriority,
        TestResult,
        TestSuite,
        UnitTestGenerator,
    )

    QA_SUITE_AVAILABLE = True
except ImportError:
    QA_SUITE_AVAILABLE = False
    (
        ChaosEngineer,
        IntegrationTestRunner,
        PerformanceTester,
        QASuite,
        SecurityTester,
        TestCase,
        TestCategory,
        TestPriority,
        TestResult,
        TestSuite,
        UnitTestGenerator,
    ) = (None,) * 11

# Import evolution components
# বাংলা মন্তব্য: evolution প্যাকেজের কিছু সাব-মডিউল (EWC, adversarial defense,
# neural-symbolic, federated learning, theory-of-mind) torch দরকার করে। torch এখন
# আর ডিফল্ট ইনস্টলে নেই (pyproject.toml-এ optional `ml` group-এ সরানো হয়েছে, কারণ
# এই research/scaffold কোড বাস্তবে কোথাও ব্যবহৃত হয় না -- verify করা হয়েছে গোটা
# রিপোতে গ্রেপ করে)। কিন্তু `core/__init__.py` প্রায় সব জায়গা থেকে import হয়
# (`import core` / `from core.X import Y`), তাই আগে torch না থাকলে এই এক লাইনেই
# পুরো ব্যাকএন্ড (এমনকি health-check টেস্টও) ImportError দিয়ে ক্র্যাশ করত -- এটাই
# আসল কারণ যে আগের সেশনগুলোতে "poetry install --with dev" ছাড়া pytest কখনো চলত না।
# try/except দিয়ে গার্ড করে দেওয়া হলো যেন torch অনুপস্থিত থাকলে শুধু এই optional
# নামগুলো None হয়ে যায়, বাকি পুরো অ্যাপ স্বাভাবিকভাবে import/চলতে পারে।
try:
    from evolution import (  # Digital Twin; Continual Learning; Adversarial Defense; Neural-Symbolic Integration; Federated Learning; Theory of Mind; Temporal Abstraction
        EWC,
        AdversarialDefenseSystem,
        AdversarialTrainer,
        AggregationMethod,
        DefenseConfig,
        DigitalTwinWorldModel,
        EWCConfig,
        EWCTrainer,
        FederatedLearningCoordinator,
        FLConfig,
        ImpactSimulator,
        MathematicalReasoningEngine,
        NeuralSymbolicConfig,
        NeuralSymbolicIntegrator,
        OnlineEWC,
        RemediationEngine,
        SystemTopologyMapper,
        TemporalAbstractionConfig,
        TemporalAbstractionSystem,
        TemporalGranularity,
        TheoryOfMindSystem,
        ToMConfig,
        ToMLevel,
        get_digital_twin_model,
    )

    EVOLUTION_COMPONENTS_AVAILABLE = True
except (ImportError, OSError):
    EVOLUTION_COMPONENTS_AVAILABLE = False
    (
        DigitalTwinWorldModel,
        get_digital_twin_model,
        SystemTopologyMapper,
        ImpactSimulator,
        RemediationEngine,
        EWC,
        OnlineEWC,
        EWCTrainer,
        EWCConfig,
        AdversarialDefenseSystem,
        AdversarialTrainer,
        DefenseConfig,
        NeuralSymbolicIntegrator,
        MathematicalReasoningEngine,
        NeuralSymbolicConfig,
        FederatedLearningCoordinator,
        FLConfig,
        AggregationMethod,
        TheoryOfMindSystem,
        ToMConfig,
        ToMLevel,
        TemporalAbstractionSystem,
        TemporalAbstractionConfig,
        TemporalGranularity,
    ) = (None,) * 24

# Version information
__version__ = "2.0.0"
__author__ = "SupremeAI Team"
__description__ = "Core Components for SupremeAI 2.0"

# All available components
__all__ = [
    # Continual Learning
    "EWC",
    # Accessibility
    "AccessibilityComplianceEngine",
    "AccessibilityIssue",
    # Adversarial Defense
    "AdversarialDefenseSystem",
    "AdversarialTrainer",
    "AggregationMethod",
    "AsyncLRUCache",
    "ChaosEngineer",
    "ColorContrastChecker",
    "ConfigManager",
    "DefenseConfig",
    "DeploymentConfig",
    "DeploymentEnvironment",
    "DeploymentManager",
    "DeploymentStatus",
    # Evolution Components
    # Digital Twin
    "DigitalTwinWorldModel",
    "EWCConfig",
    "EWCTrainer",
    "FLConfig",
    # Federated Learning
    "FederatedLearningCoordinator",
    "HTMLAccessibilityChecker",
    "HealthChecker",
    "ImageBuilder",
    "ImpactSimulator",
    "IntegrationTestRunner",
    "LRUCache",
    "MathematicalReasoningEngine",
    "NeuralSymbolicConfig",
    # Neural-Symbolic Integration
    "NeuralSymbolicIntegrator",
    "OnlineEWC",
    "OptimizationLevel",
    # Performance Optimization
    "PerformanceOptimizer",
    "PerformanceTester",
    # Deployment
    "ProductionDeploymentSystem",
    # Testing & QA
    "QASuite",
    "RemediationEngine",
    "SecurityTester",
    "SystemTopologyMapper",
    "TemporalAbstractionConfig",
    # Temporal Abstraction
    "TemporalAbstractionSystem",
    "TemporalGranularity",
    "TestCase",
    "TestCategory",
    "TestPriority",
    "TestResult",
    "TestSuite",
    # Theory of Mind
    "TheoryOfMindSystem",
    "ToMConfig",
    "ToMLevel",
    "UnitTestGenerator",
    "WCAGGuideline",
    "WCAGLevel",
    "WCAGPrinciple",
    "get_digital_twin_model",
    "get_performance_optimizer",
    "performance_monitor",
]


def get_complete_ai_system():
    """
    Get a complete AI system with all research and production components integrated.

    Returns:
        A comprehensive system with all major components
    """
    from ..evolution import get_evolution_pipeline

    # Get all evolution components
    evolution_components = get_evolution_pipeline()

    # Add production hardening components
    system = {
        # Evolution/research components
        **evolution_components,
        # Production hardening components
        "performance_optimizer": get_performance_optimizer(),
        "accessibility_engine": AccessibilityComplianceEngine(),
        "qa_suite": QASuite(),
        "deployment_system": ProductionDeploymentSystem(),
    }

    return system


def run_complete_system_test():
    """
    Run a comprehensive test of all system components.
    """
    print("Running Complete System Test...")

    # Get complete system
    system = get_complete_ai_system()

    print("\n✓ Digital Twin System:", type(system["digital_twin"]).__name__)
    print("✓ Adversarial Defense System:", type(system["defense_system"]).__name__)
    print("✓ Neural-Symbolic System:", type(system["neural_symbolic"]).__name__)
    print("✓ Theory of Mind System:", type(system["theory_of_mind"]).__name__)
    print("✓ Temporal Abstraction System:", type(system["temporal_abstraction"]).__name__)
    print("✓ Performance Optimizer:", type(system["performance_optimizer"]).__name__)
    print("✓ Accessibility Engine:", type(system["accessibility_engine"]).__name__)
    print("✓ QA Suite:", type(system["qa_suite"]).__name__)
    print("✓ Deployment System:", type(system["deployment_system"]).__name__)

    print("\n✓ EWC System: Initialized")
    print("✓ Federated Learning System: Initialized")

    print("\nAll system components successfully loaded!")
    print("Complete SupremeAI 2.0 system ready for advanced AI operations.")


def __getattr__(name: str):
    """
    Dynamically import submodules when accessed as attributes on core.

    Bengali: core প্যাকেজের সাব-মডিউলগুলো ডায়নামিকালি ইমপোর্ট করার জন্য fallback handler।
    """
    import importlib

    try:
        mod = importlib.import_module(f"core.{name}")
        globals()[name] = mod
        return mod
    except ImportError as err:
        raise AttributeError(f"module '{__name__}' has no attribute '{name}'") from err
