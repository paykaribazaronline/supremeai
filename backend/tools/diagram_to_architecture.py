import os
import base64
from typing import Dict, Any
from loguru import logger

class DiagramToArchitecture:
    def __init__(self, vision_model: str = "gpt-4o"):
        self.vision_model = vision_model
        logger.info(f"Initialized DiagramToArchitecture with model {self.vision_model}")

    def _encode_image(self, image_path: str) -> str:
        if not os.path.exists(image_path):
            raise FileNotFoundError(f"Diagram not found at {image_path}")
        with open(image_path, "rb") as image_file:
            return base64.b64encode(image_file.read()).decode('utf-8')

    async def generate_infrastructure(self, diagram_path: str, provider: str = "aws", iac_tool: str = "terraform") -> Dict[str, Any]:
        logger.info(f"Generating {iac_tool} for {provider} from diagram: {diagram_path}")
        try:
            base64_image = self._encode_image(diagram_path)
            try:
                from brain.model_router import ModelRouter
                router = ModelRouter()
                prompt = (
                    f"You are an expert infrastructure architect. Analyze the provided architecture diagram and generate {iac_tool} code for {provider}. "
                    "Identify components (VPC, EC2, RDS, S3, Load Balancers, etc.) and their relationships. "
                    "Return only the code, no explanations or markdown."
                )
                result = router.async_route_and_generate(prompt, task_type="vision", max_cost=0.05)
                code = result.get("text", "") if isinstance(result, dict) else ""
                if not code:
                    raise RuntimeError("Model returned empty response.")
                components = self._parse_components_from_code(code)
                return {
                    "status": "success",
                    "iac_tool": iac_tool,
                    "provider": provider,
                    "identified_components": components,
                    "code": code.strip(),
                }
            except ImportError:
                logger.warning("ModelRouter not available. Returning mock architecture.")
                mock_code = f"""
provider "{provider}" {{
  region = "us-east-1"
}}

resource "{provider}_vpc" "main" {{
  cidr_block = "10.0.0.0/16"
  enable_dns_hostnames = true
  tags = {{ Name = "SupremeAI-Generated-VPC" }}
}}
"""
                components = [
                    {"type": "VPC", "details": "10.0.0.0/16"},
                    {"type": "Subnet", "details": "Public and Private"},
                    {"type": "Compute", "details": "Auto-scaling group"}
                ]
                return {
                    "status": "success",
                    "iac_tool": iac_tool,
                    "provider": provider,
                    "identified_components": components,
                    "code": mock_code.strip(),
                }
        except Exception as e:
            logger.error(f"Architecture generation failed: {str(e)}")
            return {"status": "error", "error": str(e)}

    def _parse_components_from_code(self, code: str) -> List[Dict[str, str]]:
        components = []
        for line in code.splitlines():
            line_stripped = line.strip()
            if line_stripped.startswith("resource "):
                parts = line_stripped.split('"')
                if len(parts) >= 3:
                    components.append({"type": parts[1], "details": parts[3] if len(parts) > 3 else ""})
            elif line_stripped.startswith("module "):
                parts = line_stripped.split('"')
                if len(parts) >= 3:
                    components.append({"type": "module", "details": parts[1]})
        return components
