import argparse
import asyncio

from core.messaging.event_bus import error_event_bus
from core.resilience.circuit_breaker import (CircuitBreaker,
                                             CircuitBreakerOpenError)
from loguru import logger


async def simulated_network_call(should_fail: bool):
    """Simulates a network call to the LLM Gateway."""
    if should_fail:
        raise ConnectionError("Simulated network latency spike/timeout")
    await asyncio.sleep(0.1)  # Simulate normal latency
    return "success"


def event_listener(event):
    if event.error_type in ("CIRCUIT_RECOVERY", "CIRCUIT_OPEN"):
        logger.info(f"Event Bus Triggered: {event.error_type} - {event.message}")


error_event_bus.register_listener(event_listener)


async def run_experiment(target: str, fault: str, duration: int, loops: int):
    logger.info(f"🚀 Starting Chaos Experiment on '{target}' with fault '{fault}'")

    cb = CircuitBreaker(
        name=target, failure_threshold=3, recovery_timeout=5.0, half_open_after=5.0
    )

    for loop in range(1, loops + 1):
        logger.info(f"--- Loop {loop}/{loops} ---")

        # 1. Normal Operation
        for _ in range(2):
            try:
                await cb.call(simulated_network_call, False)
                logger.info(f"Call succeeded. State: {cb.state}")
            except (
                Exception
            ) as e:  # noqa: BLE001 — circuit breaker যেকোনো error তুলতে পারে, সবগুলো log করা দরকার
                logger.error(f"Unexpected error: {e}")

        # 2. Inject Faults to Trip Circuit Breaker
        logger.warning(f"💉 Injecting '{fault}' to trip the circuit breaker...")
        for _ in range(4):
            try:
                await cb.call(simulated_network_call, True)
            except CircuitBreakerOpenError as e:
                logger.critical(f"Circuit Breaker is OPEN: {e}")
                break
            except (
                Exception
            ) as e:  # noqa: BLE001 — fault injection phase-এ injected error সব ধরনের হতে পারে
                logger.warning(f"Call failed (Fault injected): {e}. State: {cb.state}")

        # Wait for the recovery timeout
        logger.info(f"⏳ Waiting {cb.recovery_timeout + 1}s for recovery timeout...")
        await asyncio.sleep(cb.recovery_timeout + 1)

        # 3. Recovery Phase
        logger.info("🔄 Initiating recovery calls...")
        for _ in range(2):
            try:
                await cb.call(simulated_network_call, False)
                logger.info(f"Recovery call succeeded. State: {cb.state}")
            except (
                Exception
            ) as e:  # noqa: BLE001 — recovery phase-এ half-open বা failed call যেকোনো error তুলতে পারে
                logger.error(f"Recovery call failed: {e}")

        logger.info("-" * 20)

    logger.success("✅ Chaos Experiment Completed!")
    # Wait for async logs to emit
    await asyncio.sleep(1)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Chaos Experiment Runner")
    parser.add_argument("--target", type=str, required=True, help="Target service")
    parser.add_argument("--fault", type=str, required=True, help="Fault to inject")
    parser.add_argument("--duration", type=int, required=True, help="Duration (s)")
    parser.add_argument("--loop", type=int, required=True, help="Number of loops")

    args = parser.parse_args()

    asyncio.run(run_experiment(args.target, args.fault, args.duration, args.loop))
