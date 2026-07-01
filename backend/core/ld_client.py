# Central LaunchDarkly Client Initialization
# বাংলা মন্তব্য: লঞ্চডার্কলি এজেন্টস কন্ট্রোল এবং ওপেনটেলিমেট্রি মনিটরিং কনফিগার করার জন্য সেন্ট্রাল ক্লায়েন্ট ফাইল

import os

from loguru import logger


# Safe import to handle missing packages or environments
try:
    import ldclient
    from ldai import LDAIClient
    from ldclient.config import Config
    from ldobserve import ObservabilityConfig
    from ldobserve import ObservabilityPlugin
    LD_SUPPORTED = True
except ImportError as e:
    logger.warning(f"LaunchDarkly SDK libraries not fully installed or import failed: {e}")
    LD_SUPPORTED = False

def init_ld_client() -> "LDAIClient | None":
    if not LD_SUPPORTED:
        return None
    
    sdk_key = os.getenv("LAUNCHDARKLY_SDK_KEY")
    if not sdk_key:
        logger.warning("LAUNCHDARKLY_SDK_KEY is not set in environment. LaunchDarkly integration disabled.")
        return None
        
    try:
        # বাংলা মন্তব্য: লঞ্চডার্কলি কোর ক্লায়েন্ট কনফিগারেশন এবং অবজারভেবিলিটি প্লাগইন ইন্টিগ্রেশন
        ldclient.set_config(Config(
            sdk_key,
            plugins=[
                ObservabilityPlugin(
                    ObservabilityConfig(
                        service_name=os.getenv("SERVICE_NAME", "supremeai-backend"),
                        service_version=os.getenv("SERVICE_VERSION", "2.0.0"),
                    )
                )
            ],
        ))
        logger.info("LaunchDarkly AI Client successfully initialized with Observability.")
        return LDAIClient(ldclient.get())
    except Exception as e:
        logger.error(f"Failed to initialize LaunchDarkly client: {e}")
        return None

# গ্লোবাল ক্লায়েন্ট রেফারেন্স (Global Client Reference)
ld_ai_client = init_ld_client()
