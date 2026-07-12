"""This module provides a gRPC client for interacting with the SupremeAI backend's Worker Service, which is responsible for executing tasks, retrieving their status, and logging audit events. It acts as a crucial communication layer, enabling the FastAPI engine to offload complex or long-running operations to a dedicated worker component, likely implemented in Java, thereby ensuring scalability and separation of concerns within the SupremeAI ecosystem.

Key Components:
- `WorkerGrpcClient`: Manages the gRPC connection and provides methods for interacting with the Worker Service.
- `WorkerGrpcClient.submit_task()`: Submits a new task to the worker for asynchronous processing, returning a task ID.
- `WorkerGrpcClient.get_task_status()`: Retrieves the current status and results of a previously submitted task.
- `WorkerGrpcClient.log_audit_event()`: Sends an audit log event to the worker for recording system activities.
- `worker_client`: A global instance of `WorkerGrpcClient` for convenient access throughout the application.

Dependencies:
- `json`: For serializing and deserializing task payloads and results.
- `logging`: For logging operational information and errors.
- `grpc`: The core library for gRPC communication.
- `protos.supreme_engine_pb2`: Generated protobuf message definitions for requests and responses.
- `protos.supreme_engine_pb2_grpc`: Generated gRPC service stubs for the Worker Service."""

import json
import logging
from typing import Any

import grpc

# We assume the protobuf compiler (protoc) will generate these files inside backend/protos
import protos.supreme_engine_pb2 as pb2
import protos.supreme_engine_pb2_grpc as pb2_grpc


logger = logging.getLogger(__name__)


class WorkerGrpcClient:
    def __init__(self, host: str = "localhost", port: int = 9090):
        self.channel = grpc.insecure_channel(f"{host}:{port}")
        self.stub = pb2_grpc.WorkerServiceStub(self.channel)

    def submit_task(self, task_type: str, payload: dict[str, Any], requested_by: str = "fastapi-engine") -> str | None:
        try:
            req = pb2.TaskRequest(task_type=task_type, payload_json=json.dumps(payload), requested_by=requested_by)
            response = self.stub.SubmitTask(req)
            logger.info(f"Task submitted to Java Worker. Task ID: {response.task_id}")
            return response.task_id
        except grpc.RpcError as e:
            logger.error(f"gRPC call failed: {e}")
            return None

    def get_task_status(self, task_id: str) -> dict[str, Any]:
        try:
            req = pb2.TaskStatusRequest(task_id=task_id)
            response = self.stub.GetTaskStatus(req)
            return {
                "task_id": response.task_id,
                "status": response.status,
                "result_json": json.loads(response.result_json) if response.result_json else None,
                "error_message": response.error_message,
            }
        except grpc.RpcError as e:
            logger.error(f"gRPC call failed: {e}")
            return {"status": "ERROR", "error_message": str(e)}

    def log_audit_event(self, event_type: str, user_id: str, resource: str, details: dict[str, Any]) -> bool:
        try:
            req = pb2.AuditLogRequest(event_type=event_type, user_id=user_id, resource=resource, details_json=json.dumps(details))
            response = self.stub.LogAuditEvent(req)
            return response.success
        except grpc.RpcError as e:
            logger.error(f"gRPC call failed: {e}")
            return False


# Global instance
worker_client = WorkerGrpcClient()
