import importlib
import logging
import sys
import traceback

logger = logging.getLogger(__name__)

sys.path.insert(0, "..")  # add f:\supremeai to sys.path
sys.path.insert(0, ".")  # add f:\supremeai\backend to sys.path

try:
    importlib.import_module("tests.api.test_api_config_routes")
    logger.info("SUCCESS")
except Exception:
    with open("traceback_collect.txt", "w") as f:
        traceback.print_exc(file=f)
    logger.info("FAILED")
