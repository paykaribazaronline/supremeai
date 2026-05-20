.PHONY: help deploy infra deploy-all clean test

help:
	@echo "SupremeAI Make Targets"
	@echo "======================"
	@echo ""
	@echo "Deployment:"
	@echo "  make infra        - Setup GCP infrastructure (service accounts, APIs, IAM)"
	@echo "  make deploy       - Deploy backend + dashboard to Cloud Run & Firebase Hosting"
	@echo "  make deploy-all   - Run infra setup + full deployment"
	@echo ""
	@echo "Development:"
	@echo "  make build        - Build backend JAR and dashboard"
	@echo "  make test         - Run all tests"
	@echo "  make clean        - Clean build artifacts"
	@echo ""
	@echo "Utilities:"
	@echo "  make logs         - Stream Cloud Run logs"
	@echo "  make rollback     - Rollback Cloud Run service to previous revision"
	@echo ""

infra:
	@echo "=== Running Infrastructure Setup ==="
	@./infrastructure/setup.sh

deploy: build
	@echo "=== Deploying to Cloud Run & Firebase Hosting ==="
	@./deploy_gcp_firebase.sh

deploy-all: infra deploy

build:
	@echo "=== Building Dashboard ==="
	@cd dashboard && npm ci && npm run build && cd ..
	@rm -rf public/admin/*
	@mkdir -p public/admin
	@cp -r dashboard/dist/* public/admin/
	@rm -rf src/main/resources/static/*
	@mkdir -p src/main/resources/static
	@cp -r dashboard/dist/* src/main/resources/static/
	@echo ""
	@echo "=== Building Backend ==="
	@./gradlew clean build -x test
	@echo "✅ Build complete"

test:
	@./gradlew test

clean:
	@./gradlew clean
	@rm -rf dashboard/dist
	@rm -rf public/admin/*
	@echo "✅ Clean complete"

logs:
	@BACKEND_URL=$$(gcloud run services describe supremeai-backend --region us-central1 --format='value(status.url)'); \
	echo "Streaming logs from $$BACKEND_URL"; \
	gcloud logging tail "resource.type=cloud_run_revision AND resource.labels.service_name=supremeai-backend" --project=$$(gcloud config get-value project)

rollback:
	@echo "Rolling back Cloud Run service..."
	@gcloud run services revert supremeai-backend --region us-central1
	@echo "✅ Rollback complete"
