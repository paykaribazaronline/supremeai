.PHONY: help tf-plan tf-apply deploy test test-e2e smoke clean logs ml-build ml-deploy show-urls

# ── Variables ────────────────────────────────────────────────────────────────
GCP_PROJECT_ID   ?= $(shell gcloud config get-value project 2>/dev/null || echo "UNSET")
REGION           ?= us-central1
TF_DIR           := terraform
ML_DIR           := ../ml-service
TEST_SCRIPT      := tests/e2e_sentiment_test.sh

# ── Help ─────────────────────────────────────────────────────────────────────
help:
	@echo "Sentiment Analysis API — Make targets"
	@echo ""
	@echo "  make tf-plan          Terraform plan"
	@echo "  make tf-apply         Terraform apply"
	@echo "  make deploy           Full deploy (Terraform + ML image + smoke tests)"
	@echo "  make ml-build         Build ML sentiment Docker image"
	@echo "  make ml-deploy        Push + deploy ML service to Cloud Run"
	@echo "  make smoke            Quick URL reachability check"
	@echo "  make test             Run full e2e test suite"
	@echo "  make test-e2e         Alias for test (shortcut)"
	@echo "  make show-urls        Print Terraform outputs"
	@echo "  make logs     Tail Cloud Run n8n logs"
	@echo "  make ml-logs          Tail Cloud Run ML service logs"
	@echo "  make clean            Remove Terraform state (destructive)"
	@echo ""

# ── Terraform ─────────────────────────────────────────────────────────────────
tf-plan:
	cd $(TF_DIR) && terraform init -upgrade && terraform plan -out=tfplan

tf-apply:
	cd $(TF_DIR) && terraform apply -auto-approve

deploy:
	@echo "Deploying with GCP_PROJECT_ID=$(GCP_PROJECT_ID) REGION=$(REGION)"
	GCP_PROJECT_ID=$(GCP_PROJECT_ID) GCP_REGION=$(REGION) ./deploy.sh

# ── ML Service ────────────────────────────────────────────────────────────────
ml-build:
	cd $(ML_DIR) && \
	  docker build -t gcr.io/$(GCP_PROJECT_ID)/sentiment-ml:latest .

ml-push: ml-build
	docker push gcr.io/$(GCP_PROJECT_ID)/sentiment-ml:latest

ml-deploy: ml-push
	gcloud run deploy sentiment-ml \
	  --image gcr.io/$(GCP_PROJECT_ID)/sentiment-ml:latest \
	  --region $(REGION) \
	  --project $(GCP_PROJECT_ID) \
	  --no-allow-unauthenticated \
	  --cpu 2 --memory 4Gi \
	  --max-instances 20 \
	  --min-instances 0 \
	  --timeout 300 \
	  --concurrency 10

# ── Tests ──────────────────────────────────────────────────────────────────────
smoke:
	@echo "Smoke-checking services…"
	@N8N=$$(cd $(TF_DIR) && terraform output -raw n8n_public_url 2>/dev/null || echo "?"); \
	ML=$$(cd $(TF_DIR) && terraform output -raw ml_service_url 2>/dev/null || echo "?"); \
	if command -v curl >/dev/null 2>&1; then \
	  echo "  n8n health:  $$(curl -sSfo /dev/null -w '%{http_code}' $$N8N/healthz 2>/dev/null || echo 000)"; \
	  echo "  ML  health:  $$(curl -sSfo /dev/null -w '%{http_code}' "$${ML}/health" 2>/dev/null || echo 000)"; \
	else \
	  echo "  curl not found."; \
	fi

test: test-e2e

test-e2e:
	@echo "Running e2e tests..." && \
	API_KEY=$$(gcloud secrets versions access latest \
	  --project=$(GCP_PROJECT_ID) --secret=ml-api-gateway-key 2>/dev/null || echo "") && \
	N8N=$$(cd $(TF_DIR) && terraform output -raw n8n_public_url 2>/dev/null || echo "") && \
	ML=$$(cd $(TF_DIR) && terraform output -raw ml_service_url 2>/dev/null || echo "") && \
	bash $(TEST_SCRIPT)

# ── Logs ──────────────────────────────────────────────────────────────────────
logs:
	@N8N=$$(cd $(TF_DIR) && terraform output -raw n8n_service_name 2>/dev/null || echo n8n-workflow); \
	gcloud logging read "resource.labels.service_name=$$N8N" \
	  --project $(GCP_PROJECT_ID) --limit 100 --format json

ml-logs:
	gcloud logging read "resource.labels.service_name=sentiment-ml" \
	  --project $(GCP_PROJECT_ID) --limit 100 --format json

# ── URLs ──────────────────────────────────────────────────────────────────────
show-urls:
	@cd $(TF_DIR) && terraform output

# ── Cleanup ──────────────────────────────────────────────────────────────────
clean:
	@echo "WARNING: This destroys the Terraform state. Ctrl+C to abort."
	@read -p "Are you sure? [y/N] " -r ans && test "$$ans" = "y"
	cd $(TF_DIR) && terraform destroy -auto-approve
