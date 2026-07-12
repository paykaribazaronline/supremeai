# বাংলা মন্তব্য: Diagram-to-Architecture টুলের Terraform/K8s output ফাংশনালিটি টেস্ট।

from unittest.mock import MagicMock, patch

import pytest

from tools.diagram_to_architecture import DiagramToArchitecture


@pytest.fixture
def mock_diagram_converter():
    with patch("tools.diagram_to_architecture.settings") as mock_settings:
        mock_settings.openai_api_key = "test-key"
        yield


@pytest.mark.anyio
async def test_to_terraform(mock_diagram_converter):
    # বাংলা মন্তব্য: Cloud architecture diagram থেকে Terraform HCL জেনারেশন টেস্ট
    converter = DiagramToArchitecture()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = """
resource "google_compute_instance" "web_server" {
  name         = "web-server-instance"
  machine_type = "e2-medium"
  zone         = "us-central1-a"

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-11"
    }
  }

  network_interface {
    network = "default"
    access_config {}
  }
}
"""
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.to_terraform("architecture_diagram.png", cloud_provider="gcp")

    assert result is not None
    assert "google_compute_instance" in result.hcl
    assert "web_server" in result.hcl


@pytest.mark.anyio
async def test_to_kubernetes(mock_diagram_converter):
    # বাংলা মন্তব্য: Architecture diagram থেকে Kubernetes YAML জেনারেশন টেস্ট
    converter = DiagramToArchitecture()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: web-app
  template:
    metadata:
      labels:
        app: web-app
    spec:
      containers:
      - name: web
        image: nginx:latest
        ports:
        - containerPort: 80
"""
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.to_kubernetes("architecture_diagram.png")

    assert result is not None
    assert "Deployment" in result.yaml
    assert "web-app" in result.yaml


@pytest.mark.anyio
async def test_to_database_schema(mock_diagram_converter):
    # বাংলা মন্তব্য: ER diagram থেকে SQLAlchemy model জেনারেশন টেস্ট
    converter = DiagramToArchitecture()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = """
from sqlalchemy import Column, Integer, String, ForeignKey
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

class User(Base):
    __tablename__ = 'users'
    id = Column(Integer, primary_key=True)
    name = Column(String(100))
    email = Column(String(255), unique=True)
"""
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.to_database_schema("er_diagram.png", orm="sqlalchemy")

    assert result is not None
    assert "User" in result.code
    assert "sqlalchemy" in result.orm.lower()


@pytest.mark.anyio
async def test_generate_api_spec(mock_diagram_converter):
    # বাংলা মন্তব্য: Flowchart থেকে API spec (OpenAPI) জেনারেশন টেস্ট
    converter = DiagramToArchitecture()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = """
openapi: 3.0.0
info:
  title: Generated API
  version: 1.0.0
paths:
  /users:
    get:
      summary: List all users
      responses:
        '200':
          description: A list of users
"""
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.generate_api_spec("flowchart.png")

    assert result is not None
    assert "openapi" in result.spec
    assert "/users" in result.spec


@pytest.mark.anyio
async def test_to_docker_compose(mock_diagram_converter):
    # বাংলা মন্তব্য: Architecture diagram থেকে Docker Compose YAML জেনারেশন টেস্ট
    converter = DiagramToArchitecture()

    with patch.object(converter, "_get_vision_client") as mock_client:
        mock_response = MagicMock()
        mock_response.choices = [MagicMock()]
        mock_response.choices[0].message.content = """
version: '3.8'
services:
  web:
    image: nginx:latest
    ports:
      - "80:80"
  database:
    image: postgres:15
    environment:
      POSTGRES_PASSWORD: example
"""
        mock_client.return_value.chat.completions.create.return_value = mock_response

        result = await converter.to_docker_compose("architecture_diagram.png")

    assert result is not None
    assert "services" in result.yaml
    assert "web" in result.yaml
