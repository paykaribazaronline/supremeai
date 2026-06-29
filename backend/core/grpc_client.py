import grpc
import json
import logging
from typing import Dict, Any, Optional

# We assume the protobuf compiler (protoc) will generate these files inside backend/protos
import protos.supreme_engine_pb2 as pb2
import protos.supreme_engine_pb2_grpc as pb2_grpc

logger = logging.getLogger(__name__)

class WorkerGrpcClient:
    def __init__(self, host: str = "localhost", port: int = 9090):
        self.channel = grpc.insecure_channel(f"{host}:{port}")
        self.stub = pb2_grpc.WorkerServiceStub(self.channel)
        
    def submit_task(self, task_type: str, payload: Dict[str, Any], requested_by: str = "fastapi-engine") -> Optional[str]:
        try:
            req = pb2.TaskRequest(
                task_type=task_type,
                payload_json=json.dumps(payload),
                requested_by=requested_by
            )
            response = self.stub.SubmitTask(req)
            logger.info(f"Task submitted to Java Worker. Task ID: {response.task_id}")
            return response.task_id
        except grpc.RpcError as e:
            logger.error(f"gRPC call failed: {e}")
            return None

    def get_task_status(self, task_id: str) -> Dict[str, Any]:
        try:
            req = pb2.TaskStatusRequest(task_id=task_id)
            response = self.stub.GetTaskStatus(req)
            return {
                "task_id": response.task_id,
                "status": response.status,
                "result_json": json.loads(response.result_json) if response.result_json else None,
                "error_message": response.error_message
            }
        except grpc.RpcError as e:
            logger.error(f"gRPC call failed: {e}")
            return {"status": "ERROR", "error_message": str(e)}

    def log_audit_event(self, event_type: str, user_id: str, resource: str, details: Dict[str, Any]) -> bool:
        try:
            req = pb2.AuditLogRequest(
                event_type=event_type,
                user_id=user_id,
                resource=resource,
                details_json=json.dumps(details)
            )
            response = self.stub.LogAuditEvent(req)
            return response.success
        except grpc.RpcError as e:
            logger.error(f"gRPC call failed: {e}")
            return False

# Global instance
worker_client = WorkerGrpcClient()
