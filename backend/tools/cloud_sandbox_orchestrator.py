import os
import uuid
from typing import Any

import httpx
from loguru import logger


class CloudSandboxOrchestrator:
    """
    Orchestrates persistent Ubuntu VMs/Containers for safe and isolated code execution.
    Supports local Docker and RunPod for cloud-scale execution.
    Maintains state across sessions.
    """

    def __init__(self, provider: str = "auto"):
        self.provider = provider
        self.active_sessions: dict[str, dict[str, Any]] = {}

        if self.provider == "auto":
            if os.getenv("RUNPOD_API_KEY"):
                self.provider = "runpod"
            elif os.getenv("DOCKER_HOST") or os.path.exists("/var/run/docker.sock") or os.name == "nt":
                self.provider = "docker"
            else:
                self.provider = "local"

        logger.info(f"Initialized CloudSandboxOrchestrator with provider: {self.provider}")
        self._docker_client = None

    async def _get_docker_client(self):
        if self._docker_client:
            return self._docker_client
        if self.provider not in {"docker", "local"}:
            return None
        try:
            import docker

            self._docker_client = docker.from_env()
            self._docker_client.ping()
            return self._docker_client
        except Exception as e:
            logger.warning(f"Docker not available locally: {e}. Falling back to mock/local.")
            return None

    async def create_session(self, image: str = "ubuntu:22.04") -> str:
        session_id = f"sandbox-{uuid.uuid4().hex[:8]}"
        logger.info(f"Creating {self.provider} session: {session_id} with image {image}")

        if self.provider in ["docker", "auto", "local"]:
            client = await self._get_docker_client()
            if client:
                try:
                    container = client.containers.run(
                        image,
                        detach=True,
                        tty=True,
                        stdin_open=True,
                    )
                    session_data = {
                        "image": image,
                        "status": "running",
                        "provider": "docker",
                        "cwd": "/workspace",
                        "env": {"PYTHONUNBUFFERED": "1"},
                        "container_id": container.id,
                    }
                    self.active_sessions[session_id] = session_data
                    logger.success(f"Docker session created successfully: {session_id} (Container ID: {container.id})")
                    return session_id
                except Exception as e:
                    logger.error(f"Failed to create Docker container: {e}")
                    if self.provider != "auto":
                        raise  # Only fallback if in auto mode

        if self.provider in ["runpod", "auto"]:
            api_key = os.getenv("RUNPOD_API_KEY")
            if not api_key:
                logger.warning("RUNPOD_API_KEY not found, cannot fallback to RunPod.")
                if self.provider == "runpod":
                    raise RuntimeError("RUNPOD_API_KEY is required for RunPod sandbox sessions.")
            else:
                headers = {
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                }
                payload = {
                    "name": session_id,
                    "imageName": image,
                    "gpuTypeId": "cpu",  # Serverless/CPU pod for cost savings
                    "volumeInGb": 5,
                    "ports": "22/tcp,80/tcp",
                }
                async with httpx.AsyncClient() as http_client:
                    resp = await http_client.post(
                        "https://api.runpod.io/v1/user/pod",
                        json=payload,
                        headers=headers,
                        timeout=20.0,
                    )
                    if resp.status_code in (200, 201):
                        data = resp.json()
                        pod_id = data.get("id")
                        self.active_sessions[session_id] = {
                            "image": image,
                            "status": "running",
                            "provider": "runpod",
                            "pod_id": pod_id,
                            "cwd": "/workspace",
                            "env": {"PYTHONUNBUFFERED": "1"},
                        }
                        logger.success(f"RunPod session created successfully: {session_id} (Pod ID: {pod_id})")
                        return session_id
                    else:
                        logger.error(f"RunPod pod creation API failed: {resp.text}. Falling back to local mock.")

        logger.warning(f"All providers failed. Creating a mock session for {session_id}.")
        self.active_sessions[session_id] = {
            "image": image,
            "status": "running",
            "provider": self.provider,
            "cwd": "/workspace",
            "env": {"PYTHONUNBUFFERED": "1"},
        }
        return session_id

    async def run_command(self, session_id: str, command: str, timeout: int = 60) -> dict[str, Any]:
        if session_id not in self.active_sessions:
            raise ValueError(f"Session {session_id} not found or inactive.")

        session = self.active_sessions[session_id]
        logger.info(f"[{session_id}] Executing: {command}")

        provider = session.get("provider", self.provider)
        if provider == "docker":
            container_id = session.get("container_id")
            if container_id:
                client = await self._get_docker_client()
                if client:
                    try:
                        container = client.containers.get(container_id)
                        exec_result = container.exec_run(
                            f"bash -lc '{command}'",
                            workdir=session.get("cwd", "/workspace"),
                        )
                        exit_code = exec_result.exit_code
                        output = exec_result.output.decode("utf-8", errors="replace")
                        return {"exit_code": exit_code, "stdout": output, "stderr": ""}
                    except Exception as e:
                        logger.error(f"Docker exec failed: {e}")
                        return {"exit_code": 1, "stdout": "", "stderr": str(e)}

        if command.startswith("cd "):
            new_dir = command.split("cd ")[1].strip()
            session["cwd"] = os.path.join(session["cwd"], new_dir) if session["cwd"] != "/workspace" else os.path.join("/workspace", new_dir)
            return {"exit_code": 0, "stdout": "", "stderr": ""}

        if provider == "runpod":
            api_key = os.getenv("RUNPOD_API_KEY")
            pod_id = session.get("pod_id")
            if api_key and pod_id:
                headers = {
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                }
                payload = {"command": f"cd {session['cwd']} && {command}"}
                async with httpx.AsyncClient() as http_client:
                    # RunPod execute command endpoint
                    resp = await http_client.post(
                        f"https://api.runpod.io/v1/pod/{pod_id}/cmd",
                        json=payload,
                        headers=headers,
                        timeout=float(timeout),
                    )
                    if resp.status_code == 200:
                        data = resp.json()
                        return {
                            "exit_code": data.get("exitCode", 0),
                            "stdout": data.get("stdout", ""),
                            "stderr": data.get("stderr", ""),
                        }

        return {
            "exit_code": 0,
            "stdout": f"Mock output for '{command}' in {provider} sandbox at {session['cwd']}",
            "stderr": "",
        }

    async def write_file(self, session_id: str, filepath: str, content: str) -> bool:
        if session_id not in self.active_sessions:
            raise ValueError(f"Session {session_id} not found.")
        logger.info(f"[{session_id}] Writing file: {filepath}")

        provider = self.active_sessions[session_id].get("provider", self.provider)
        if provider == "docker":
            container_id = self.active_sessions[session_id].get("container_id")
            if container_id:
                client = await self._get_docker_client()
                if client:
                    try:
                        container = client.containers.get(container_id)
                        import base64

                        encoded = base64.b64encode(content.encode("utf-8")).decode("utf-8")
                        exec_result = container.exec_run(f"echo '{encoded}' | base64 -d > {filepath}")
                        return exec_result.exit_code == 0
                    except Exception as e:
                        logger.error(f"Docker file write failed: {e}")
                        return False
        return True

    async def read_file(self, session_id: str, filepath: str) -> str:
        if session_id not in self.active_sessions:
            raise ValueError(f"Session {session_id} not found.")
        logger.info(f"[{session_id}] Reading file: {filepath}")

        provider = self.active_sessions[session_id].get("provider", self.provider)
        if provider == "docker":
            container_id = self.active_sessions[session_id].get("container_id")
            if container_id:
                client = await self._get_docker_client()
                if client:
                    try:
                        container = client.containers.get(container_id)
                        exec_result = container.exec_run(f"cat {filepath}")
                        return exec_result.output.decode("utf-8", errors="replace")
                    except Exception as e:
                        logger.error(f"Docker file read failed: {e}")
                        return ""
        return f"# Content of {filepath}"

    async def terminate_session(self, session_id: str):
        if session_id in self.active_sessions:
            logger.info(f"Terminating session {session_id}")
            session = self.active_sessions[session_id]
            provider = session.get("provider", self.provider)

            if provider == "docker":
                container_id = session.get("container_id")
                if container_id:
                    client = await self._get_docker_client()
                    if client:
                        try:
                            container = client.containers.get(container_id)
                            container.stop(timeout=5)
                            container.remove()
                        except Exception as e:
                            logger.warning(f"Docker cleanup failed: {e}")

            elif provider == "runpod":
                api_key = os.getenv("RUNPOD_API_KEY")
                pod_id = session.get("pod_id")
                if api_key and pod_id:
                    headers = {"Authorization": f"Bearer {api_key}"}
                    async with httpx.AsyncClient() as http_client:
                        await http_client.post(
                            f"https://api.runpod.io/v1/pod/{pod_id}/stop",
                            headers=headers,
                            timeout=15.0,
                        )

            self.active_sessions.pop(session_id)
            return True
        return False
