import httpx

from adaptive_engine.experience_db import ExperienceDatabase
from adaptive_engine.intent_parser import IntentParser
from adaptive_engine.platform_learner import PlatformLearner
from adaptive_engine.registry import PlatformRegistry
from admin.god import AdminGodLayer
from brain.gcp_router import GCPCloudRunRouter
from brain.model_router import ModelRouter
from brain.parallel_cloud_router import ParallelCloudRouter
from core.config import settings
from core.gcp_firestore import GCPFirestoreVerificationQueue
from core.gcp_pubsub_queue import GCPPubSubQueue
from core.intent import IntentClassifier
from core.universal_rules import UniversalRulesEngine
from core.upstash_redis_queue import UpstashRedisQueue
from tools.gcp_cloud_functions import GCPCloudFunctionClient


global_http_client: httpx.AsyncClient | None = None

model_router = ModelRouter()
intent_clf = IntentClassifier()
admin_god = AdminGodLayer(db_path=settings.admin_rules_db)
parallel_router = ParallelCloudRouter()
gcp_router = GCPCloudRunRouter()
verification_queue = GCPFirestoreVerificationQueue()
gcp_pubsub_queue = GCPPubSubQueue()
cloud_function_client = GCPCloudFunctionClient()
redis_queue = UpstashRedisQueue()

platform_registry = PlatformRegistry()
experience_db = ExperienceDatabase()
intent_parser = IntentParser(model_router)
platform_learner = PlatformLearner(model_router, platform_registry)

rules_engine = UniversalRulesEngine()
