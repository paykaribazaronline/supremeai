# backend/scripts/self_test_and_improve.py
"""SupremeAI Self-Test & Auto-Improve Runner.

Continuous self-improvement loop:
  1. Run benchmarks
  2. Analyze results
  3. Detect limits & weaknesses
  4. Auto-optimize
  5. Re-test to validate
  6. Repeat forever (or until stopped)
"""

from __future__ import annotations

import argparse
import asyncio
from datetime import datetime
import os
import sys

# Ensure backend root is on sys.path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from core.adaptive_optimizer import AdaptiveOptimizer
from core.integration_layer import SupremeAIIntegrator
from core.self_benchmark import BenchmarkCategory, SelfBenchmarkEngine


async def run_self_test_cycle(ai_system: Any, benchmarker: SelfBenchmarkEngine, optimizer: AdaptiveOptimizer, quick_mode: bool = False) -> dict:
    """Run one complete self-test and improvement cycle."""
    print("\n" + "=" * 70)
    print(f"🧪 SUPREMEAI SELF-TEST CYCLE - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)

    categories = [BenchmarkCategory.PERFORMANCE, BenchmarkCategory.ACCURACY] if quick_mode else None
    benchmark_report = await benchmarker.run_full_benchmark(categories=categories)

    print(f"\n📈 BENCHMARK RESULTS:")
    print(f"   Overall Score: {benchmark_report.overall_score:.1%} (Grade: {benchmark_report.grade})")
    print(f"   Tests Passed: {benchmark_report.summary['tests_passed']}/{benchmark_report.summary['tests_total']}")
    print(f"   Weaknesses: {benchmark_report.summary['weakness_count']} found")

    optimization_result = None
    if benchmark_report.improvements_needed or benchmark_report.overall_score < 0.95:
        print("\n🔧 PHASE 2: Running Auto-Optimization...")
        optimization_result = await optimizer.optimize_based_on_benchmark(benchmark_report)
        print(f"   Actions Applied: {len(optimization_result.actions_taken)}")
        print(f"   Overall Improvement: {optimization_result.overall_improvement:+.1%}")

    return {
        "benchmark": benchmark_report,
        "optimization": optimization_result,
    }


async def main() -> None:
    parser = argparse.ArgumentParser(description="SupremeAI Self-Test & Auto-Improve")
    parser.add_argument("--quick", action="store_true", help="Run quick benchmark")
    parser.add_argument("--continuous", action="store_true", help="Run continuous improvement loop")
    parser.add_argument("--interval", type=int, default=30, help="Interval between cycles in minutes")
    parser.add_argument("--max-cycles", type=int, default=1, help="Maximum number of cycles")
    args = parser.parse_args()

    print("🧠 SUPREMEAI SELF-TEST & AUTO-IMPROVE SYSTEM")
    ai_system = SupremeAIIntegrator()
    await ai_system.initialize()

    benchmarker = SelfBenchmarkEngine(ai_system=ai_system)
    optimizer = AdaptiveOptimizer(benchmarker=benchmarker, ai_system=ai_system)

    cycle_count = 0
    while True:
        cycle_count += 1
        await run_self_test_cycle(ai_system, benchmarker, optimizer, quick_mode=args.quick)
        if not args.continuous or (args.max_cycles and cycle_count >= args.max_cycles):
            break
        await asyncio.sleep(args.interval * 60)

    await ai_system.shutdown()


if __name__ == "__main__":
    asyncio.run(main())
