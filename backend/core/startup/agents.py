import asyncio
from loguru import logger
from core import services
from core.agent_supervisor import agent_supervisor


async def start_background_services(app):
    # ── Start background agents via centralized Supervisor ────────────────────
    from core.cache.multi_layer_cache import start_swarm_cache_invalidator
    from core.sentinel_agent import sentinel

    # Agent 1: Sentinel Agent (periodic endpoint monitoring & dependency audit)
    await agent_supervisor.start_agent(
        "sentinel",
        lambda: sentinel.run_periodic_loop(),
        health_check_interval=60,
        max_restarts=10,
        restart_delay=1.0,
    )

    # Agent 2: Swarm Cache Invalidator (multi-layer cache maintenance)
    await agent_supervisor.start_agent(
        "swarm-cache",
        start_swarm_cache_invalidator,
        health_check_interval=60,
        max_restarts=5,
        restart_delay=5.0,
    )

    try:
        from core.telemetry.system_telemetry import run_system_telemetry_loop

        await agent_supervisor.start_agent(
            "system-telemetry",
            run_system_telemetry_loop,
            health_check_interval=60,
            max_restarts=5,
            restart_delay=2.0,
        )
        logger.info("✅ System Telemetry Broadcaster background loop started.")
    except Exception as exc:
        logger.warning(f"⚠️ System Telemetry Broadcaster failed to start: {exc}")

    # Agent 4: Bug Prophet Anomaly Detector
    try:
        from scripts.devops.bug_prophet import run_anomaly_detector_loop

        await agent_supervisor.start_agent(
            "bug-prophet-anomaly-detector",
            run_anomaly_detector_loop,
            health_check_interval=60,
            max_restarts=5,
            restart_delay=5.0,
        )
        logger.info("✅ BugProphet Anomaly Detector started.")
    except Exception as exc:
        logger.warning(f"⚠️ BugProphet Anomaly Detector failed to start: {exc}")

    import os

    # Start Tier-8 Meta-Self Agents
    try:
        if os.getenv("ENABLE_TIER8", "false").lower() == "true":
            from core.tier8.tier8_integration import init_tier8

            await init_tier8(services.registry)
            logger.info("✅ Tier-8 Meta-Self subsystem initialized successfully.")
        else:
            logger.info("ℹ️ Tier-8 Meta-Self subsystem disabled via environment variable.")
    except Exception as exc:
        logger.warning(f"⚠️ Tier-8 initialization failed: {exc}")

    # বাংলা মন্তব্ব্য: SelfEvolutionAgent শুরু করা — এখন AgentSupervisor-এর অধীনে চলবে।
    try:
        if os.getenv("ENABLE_EVOLUTION", "false").lower() == "true":
            from core.evolution.self_evolution_agent import SelfEvolutionAgent

            _evo_agent = SelfEvolutionAgent(interval_seconds=300)
            await _evo_agent.start()
            app.state.evo_agent = _evo_agent
            logger.info("✅ SelfEvolutionAgent background loop started (5-min evolution cycle).")
        else:
            app.state.evo_agent = None
            logger.info("ℹ️ SelfEvolutionAgent disabled via environment variable.")
    except Exception as exc:
        logger.warning(f"⚠️ SelfEvolutionAgent failed to start: {exc}")
        app.state.evo_agent = None

    # বাংলা মন্তব্ব্য: DailyLearner শুরু করা — এখন AgentSupervisor-এর অধীনে চলবে।
    try:
        if os.getenv("ENABLE_DAILY_LEARNER", "false").lower() == "true":
            from core.evolution.daily_learner import DailyLearner

            _daily_learner = DailyLearner()
            async def _daily_learner_loop() -> None:
                while True:
                    try:
                        await _daily_learner.learn_and_plan(
                            "Improve SupremeAI agent reasoning, error recovery, and free-tier efficiency"
                        )
                    except Exception as _exc:
                        logger.warning(f"⚠️ DailyLearner cycle failed: {_exc}")
                    await asyncio.sleep(86400)
            await agent_supervisor.start_agent(
                "daily-learner",
                lambda: _daily_learner_loop(),
                health_check_interval=3600,  # Check hourly (runs every 24h)
                max_restarts=5,
                restart_delay=60.0,
            )
            logger.info("✅ DailyLearner background task started (24h research scan cycle).")
        else:
            logger.info("ℹ️ DailyLearner disabled via environment variable.")
    except Exception as exc:
        logger.warning(f"⚠️ DailyLearner failed to start: {exc}")

    # বাংলা মন্তব্ব্য: AutoHealerService শুরু করা — DB/Redis স্বয়ংক্রিয়ভাবে ঠিক করে।
    try:
        if os.getenv("ENABLE_AUTO_HEALER", "true").lower() == "true":
            from core.errors.auto_healer import auto_healer_service

            await auto_healer_service.start()
            app.state.auto_healer = auto_healer_service
            logger.info("✅ AutoHealerService started (DB/Redis healing active, 30s check interval).")
        else:
            logger.info("ℹ️ AutoHealerService disabled via environment variable.")
    except Exception as exc:
        logger.warning(f"⚠️ AutoHealerService failed to start: {exc}")

    # বাংলা মন্তব্ব্য: SelfHealer error listener এক্সপ্লিসিটলি রেজিস্টার করা হচ্ছে।
    try:
        from core.health.self_healer import register_self_healer_listener

        register_self_healer_listener()
        logger.info("✅ SelfHealer error listener registered in lifespan.")
    except Exception as exc:
        logger.warning(f"⚠️ SelfHealer listener registration failed: {exc}")

    # Start the agent health monitor
    await agent_supervisor.start_monitor(check_interval=30)
