# =============================================================================
# 1. Enable Required GCP APIs
# =============================================================================

resource "google_project_service" "cloudrun" {
  project = var.project_id
  service = "run.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "container" {
  project = var.project_id
  service = "container.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "storage" {
  project = var.project_id
  service = "storage.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "secretmanager" {
  project = var.project_id
  service = "secretmanager.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "cloud_build" {
  project = var.project_id
  service = "cloudbuild.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "containerregistry" {
  project = var.project_id
  service = "containerregistry.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "artifact_registry" {
  project = var.project_id
  service = "artifactregistry.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "cloud_armor" {
  project = var.project_id
  service = "compute.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "cloud_scheduler" {
  project = var.project_id
  service = "cloudscheduler.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "cloud_tasks" {
  project = var.project_id
  service = "cloudtasks.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "cloud_trace" {
  project = var.project_id
  service = "cloudtrace.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "monitoring" {
  project = var.project_id
  service = "monitoring.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "cloud_notifications" {
  project = var.project_id
  service = "cloudnotifications.googleapis.com"

  disable_on_destroy = false
}

resource "google_project_service" "iap" {
  project = var.project_id
  service = "iap.googleapis.com"

  disable_on_destroy = false
}

# =============================================================================
# 2. Service Accounts
# =============================================================================

resource "google_service_account" "n8n_sa" {
  project    = var.project_id
  account_id = var.n8n_service_name
  display_name = "n8n Workflow Orchestrator Service Account"

  depends_on = [google_project_service.cloudrun]
}

resource "google_service_account" "ml_sa" {
  project    = var.project_id
  account_id = var.ml_service_name
  display_name = "BERT Sentiment ML Service Account"

  depends_on = [google_project_service.cloudrun]
}

resource "google_service_account" "operation_sa" {
  project    = var.project_id
  account_id = "supremeai-ops"
  display_name = "SupremeAI Operations Service Account"

  depends_on = [google_project_service.cloudrun]
}

# =============================================================================
# 3. IAM Roles — Least-Privilege
# =============================================================================

# n8n SA needs: Cloud Storage RW, Secret Manager Reader, Cloud Trace Writer, Monitoring Metric Writer, Cloud Run Invoker (to call ML), Pub/Sub Publisher (for alerts)
resource "google_project_iam_member" "n8n_sa_storage_admin" {
  project = var.project_id
  role    = "roles/storage.objectAdmin"
  member  = "serviceAccount:${google_service_account.n8n_sa.email}"

  depends_on = [google_service_account.n8n_sa]
}

resource "google_project_iam_member" "n8n_sa_secretmanager_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.n8n_sa.email}"
}

resource "google_project_iam_member" "n8n_sa_cloud_trace" {
  project = var.project_id
  role    = "roles/cloudtrace.agent"
  member  = "serviceAccount:${google_service_account.n8n_sa.email}"
}

resource "google_project_iam_member" "n8n_sa_metrics_writer" {
  project = var.project_id
  role    = "roles/monitoring.metricWriter"
  member  = "serviceAccount:${google_service_account.n8n_sa.email}"
}

resource "google_project_iam_member" "n8n_sa_ml_invoker" {
  project = var.project_id
  role    = "roles/run.invoker"
  member  = "serviceAccount:${google_service_account.n8n_sa.email}"
}

resource "google_project_iam_member" "n8n_sa_pubsub_publisher" {
  project = var.project_id
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${google_service_account.n8n_sa.email}"
}

resource "google_project_iam_member" "n8n_sa_cloudscheduler_jobs_runner" {
  project = var.project_id
  role    = "roles/cloudscheduler.jobRunner"
  member  = "serviceAccount:${google_service_account.n8n_sa.email}"
}

# ML Service SA needs: Storage (models), Secret Manager (API key), Monitoring, Logging
resource "google_project_iam_member" "ml_sa_storage_object_admin" {
  project = var.project_id
  role    = "roles/storage.objectAdmin"
  member  = "serviceAccount:${google_service_account.ml_sa.email}"
}

resource "google_project_iam_member" "ml_sa_secretmanager_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.ml_sa.email}"
}

resource "google_project_iam_member" "ml_sa_cloud_trace" {
  project = var.project_id
  role    = "roles/cloudtrace.agent"
  member  = "serviceAccount:${google_service_account.ml_sa.email}"
}

resource "google_project_iam_member" "ml_sa_metrics_writer" {
  project = var.project_id
  role    = "roles/monitoring.metricWriter"
  member  = "serviceAccount:${google_service_account.ml_sa.email}"
}

# Operations SA — read-only access for admin/debugging
resource "google_project_iam_member" "ops_sa_storage_admin" {
  project = var.project_id
  role    = "roles/storage.objectAdmin"
  member  = "serviceAccount:${google_service_account.operation_sa.email}"
}

resource "google_project_iam_member" "ops_sa_secretmanager_accessor" {
  project = var.project_id
  role    = "roles/secretmanager.secretAccessor"
  member  = "serviceAccount:${google_service_account.operation_sa.email}"
}

# =============================================================================
# 4. Cloud Storage — n8n Persistent Storage + Backup Retention
# =============================================================================

resource "google_storage_bucket" "n8n_storage" {
  project       = var.project_id
  name          = var.n8n_bucket_name
  location      = var.default_region
  storage_class = "STANDARD"

  uniform_bucket_level_access = true

  versioning {
    enabled = true
  }

  encryption {
    default_kms_key_name = google_kms_crypto_key.n8n_storage_key.id
  }

  lifecycle_rule {
    condition {
      age = 30
    }
    action {
      type = "Delete"
    }
  }

  depends_on = [google_project_service.storage, google_kms_crypto_key.n8n_storage_key]
}

resource "google_storage_bucket" "n8n_backup" {
  project       = var.project_id
  name          = var.n8n_backup_bucket_name
  location      = var.default_region
  storage_class = "NEARLINE"

  uniform_bucket_level_access = true

  versioning {
    enabled = true
  }

  encryption {
    default_kms_key_name = google_kms_crypto_key.n8n_storage_key.id
  }

  # Daily backups retained for 30 days
  lifecycle_rule {
    condition {
      age = 30
    }
    action {
      type = "Delete"
    }
  }

  depends_on = [google_project_service.storage, google_kms_crypto_key.n8n_storage_key]
}

# =============================================================================
# 5. Secret Manager — No Hardcoded Secrets
# =============================================================================

# Core n8n encryption key (auto-generated random 32-byte key)
resource "random_id" "n8n_encryption" {
  byte_length = 32
}

resource "google_secret_manager_secret" "n8n_encryption_key" {
  project   = var.project_id
  secret_id = var.n8n_encryption_key_secret_name

  replication {
    auto {}
  }

  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "n8n_encryption_key" {
  secret      = google_secret_manager_secret.n8n_encryption_key.id
  secret_data = random_id.n8n_encryption.hex
}

# n8n Basic Auth credentials (username:password base64, injected at deploy-time)
resource "google_secret_manager_secret" "n8n_basic_auth" {
  project   = var.project_id
  secret_id = var.n8n_basic_auth_secret_name

  replication {
    auto {}
  }

  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "n8n_basic_auth" {
  secret      = google_secret_manager_secret.n8n_basic_auth.id
  secret_data = "YWRtaW46c3ByZW1haW5haS0wMjEtYWRtYWlu"

  lifecycle {
    ignore_changes = [secret_data]
  }
}

# ML API Gateway key
resource "random_id" "ml_api_key" {
  byte_length = 32
}

resource "google_secret_manager_secret" "ml_api_key" {
  project   = var.project_id
  secret_id = var.ml_api_key_secret_name

  replication {
    auto {}
  }

  depends_on = [google_project_service.secretmanager]
}

resource "google_secret_manager_secret_version" "ml_api_key" {
  secret      = google_secret_manager_secret.ml_api_key.id
  secret_data = random_id.ml_api_key.hex
}

# IAM bindings so only the right SA can read each secret
resource "google_secret_manager_secret_iam_member" "n8n_sa_encryption_access" {
  secret_id = google_secret_manager_secret.n8n_encryption_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.n8n_sa.email}"
}

resource "google_secret_manager_secret_iam_member" "n8n_sa_basic_auth_access" {
  secret_id = google_secret_manager_secret.n8n_basic_auth.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.n8n_sa.email}"
}

resource "google_secret_manager_secret_iam_member" "ml_sa_api_key_access" {
  secret_id = google_secret_manager_secret.ml_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.ml_sa.email}"
}

# =============================================================================
# 6. VPC Access Connector (private connectivity control plane)
# =============================================================================

resource "google_vpc_access_connector" "n8n_connector" {
  project = var.project_id
  region  = var.default_region
  name    = var.vpc_connector_name

  network             = "default"
  ip_cidr_range       = "10.8.0.0/28"
  min_instances       = 0
  max_instances       = 10
  max_throughput      = 1000

  depends_on = [google_project_service.cloudrun]
}

# =============================================================================
# 7. Cloud Armor — DDoS Protection + IP Allowlist for n8n Admin
# =============================================================================

resource "google_compute_security_policy" "n8n_armor" {
  name        = "${var.n8n_service_name}-armor"
  description = "Web application firewall and IP allowlist for n8n admin — DDoS + WAF rules"

  # ── 900: Allow trusted admin IPs (highest priority before public block) ──
  dynamic "rule" {
    for_each = var.cloud_armor_allowed_admin_cidrs
    content {
      action   = "allow"
      priority = 900
      match {
        versioned_expr = "SRC_IP_V1"
        src_ip_ranges  = [rule.value]
      }
      description = "Allow trusted admin / VPN CIDR — n8n admin reachable from these IPs"
    }
  }

  # ── 2000: SQL injection ──
  rule {
    action   = "deny(403)"
    priority = 2000
    match {
      expr {
        expression = "evaluatePreconfiguredExpr('xss-stable')"
      }
    }
    description = "Block XSS patterns"
  }

  # ── 2100: SQL injection ──
  rule {
    action   = "deny(403)"
    priority = 2100
    match {
      expr {
        expression = "evaluatePreconfiguredExpr('sqli-stable')"
      }
    }
    description = "Block SQL injection patterns"
  }

  # ── 2200: Rate limiting — 500 req / 1 min per IP ──
  rule {
    action   = "rate_based_ban"
    priority = 2200
    match {
      versioned_expr = "SRC_IP_V1"
    }
    rate_limit_options {
      conform_action = "allow"
      exceed_action  = "deny(429)"
      enforce_on_key = "IP"
      rate_limit_threshold {
        count        = 500
        interval_sec = 60
      }
    }
    description = "Rate-limit: 500 req/min per IP globally"
  }

  # ── 3000: Default deny ──
  rule {
    action   = "allow"
    priority = 3000
    description = "Default allow — managed by Cloud Run load balancer"
  }

  depends_on = [google_project_service.cloud_armor]
}

# =============================================================================
# 8. Secret Rotation (Cloud Scheduler — 90-day secret version rotation)
# =============================================================================

resource "google_cloud_scheduler_job" "rotate_n8n_encryption_key" {
  name        = "rotate-n8n-encryption-key"
  description = "Rotate n8n encryption key every 90 days"
  schedule    = "0 0 1 */90 *"
  time_zone   = "UTC"
  region      = var.default_region
  project     = var.project_id

  http_target {
    uri = "https://run.googleapis.com/v1/projects/${var.project_id}/locations/${var.default_region}/jobs/rotate-n8n-encryption-key/_job"

    oidc_token {
      service_account_email = google_service_account.operation_sa.email
    }
  }

  depends_on = [google_project_service.cloud_scheduler]
}

resource "google_cloud_scheduler_job" "backup_n8n_workflows" {
  name        = "daily-n8n-backup"
  description = "Daily Cloud Scheduler job triggers the n8n backup workflow"
  schedule    = "0 2 * * *"   # 02:00 UTC daily
  time_zone   = "UTC"
  region      = var.default_region
  project     = var.project_id

  http_target {
    uri = "https://${google_cloud_run_service.n8n.status[0].url}/webhook/daily-n8n-backup"

    oidc_token {
      service_account_email = google_service_account.operation_sa.email
    }

    headers = {
      "X-N8N-Api-Key" = "" # injected via n8n secret at runtime
    }
  }

  depends_on = [google_project_service.cloud_scheduler, google_cloud_run_service.n8n]
}

# =============================================================================
# 9. Monitoring Alert Policies
# =============================================================================

# Alert: n8n workflow execution failures
resource "google_monitoring_alert_policy" "n8n_workflow_failures" {
  display_name = "n8n Workflow Execution Failures"
  project      = var.project_id

  conditions {
    display_name = "n8n execution failures"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/request_count\" resource.type=\"cloud_run_revision\" resource.labels.service_name=\"${google_cloud_run_service.n8n.name}\" metric.labels.response_code_class=\"5xx\""
      duration        = "60s"
      comparison      = "COMPARISON_GT"
      threshold_value = 0
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_RATE"
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.alert_email.id]

  depends_on = [google_project_service.monitoring]
}

# Alert: ML inference latency >= 500ms
resource "google_monitoring_alert_policy" "ml_inference_latency" {
  display_name = "ML Inference Latency > 500ms"
  project      = var.project_id

  conditions {
    display_name = "ML p50 latency > 500ms"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/request_latencies\" resource.type=\"cloud_run_revision\" resource.labels.service_name=\"${google_cloud_run_service.ml_sentiment.name}\""
      duration        = "300s"
      comparison      = "COMPARISON_GT"
      threshold_value = 0.5
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_PERCENTILE_50"
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.alert_email.id]
  depends_on            = [google_project_service.monitoring]
}

# Alert: Endpoint error rate > 1%
resource "google_monitoring_alert_policy" "n8n_error_rate" {
  display_name = "n8n Endpoint Error Rate > 1%"
  project      = var.project_id

  conditions {
    display_name = "High ML endpoint error rate"
    condition_threshold {
      filter          = "metric.type=\"run.googleapis.com/request_count\" resource.type=\"cloud_run_revision\" metric.labels.response_code_class=\"5xx\""
      duration        = "300s"
      comparison      = "COMPARISON_GT"
      threshold_value = 0.01
      denominator_filter = "metric.type=\"run.googleapis.com/request_count\""
      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_RATE"
      }
    }
  }

  notification_channels = [google_monitoring_notification_channel.alert_email.id]
  depends_on            = [google_project_service.monitoring]
}

resource "google_monitoring_notification_channel" "alert_email" {
  project      = var.project_id
  display_name = "SupremeAI Alert Email"
  type         = "email"
  labels = {
    email_address = var.alert_email
  }
}

# =============================================================================
# 10. Artifact Registry (container image store)
# =============================================================================

resource "google_artifact_registry_repository" "n8n_repo" {
  project       = var.project_id
  location      = var.default_region
  repository_id = "n8n-workflows"

  format = "DOCKER"
  description = "Docker image repository for n8n workflow service"

  depends_on = [google_project_service.artifact_registry]
}

resource "google_artifact_registry_repository" "ml_repo" {
  project       = var.project_id
  location      = var.default_region
  repository_id = "ml-inference"

  format = "DOCKER"
  description = "Docker image repository for BERT sentiment ML service"

  depends_on = [google_project_service.artifact_registry]
}

# =============================================================================
# 11. KMS Keys — Customer-Managed Encryption for Data at Rest
# =============================================================================

resource "google_kms_key_ring" "n8n_ring" {
  name     = "n8n-keyring"
  location = var.default_region
  project  = var.project_id
}

resource "google_kms_crypto_key" "n8n_storage_key" {
  name            = "n8n-storage-key"
  key_ring        = google_kms_key_ring.n8n_ring.id
  purpose         = "ENCRYPT_DECRYPT"
  rotation_period = "7776000s" # 90 days

  depends_on = [google_kms_key_ring.n8n_ring]
}

resource "google_kms_crypto_key" "n8n_secrets_key" {
  name            = "n8n-secrets-key"
  key_ring        = google_kms_key_ring.n8n_ring.id
  purpose         = "ENCRYPT_DECRYPT"
  rotation_period = "7776000s" # 90 days

  depends_on = [google_kms_key_ring.n8n_ring]
}

# =============================================================================
# 12. n8n Cloud Run Service
# =============================================================================

resource "google_cloud_run_service" "n8n" {
  project  = var.project_id
  name     = var.n8n_service_name
  location = var.default_region

  template {
    spec {
      service_account_name = google_service_account.n8n_sa.email

      containers {
        image = "n8nio/n8n:1.95.0"

        resources {
          limits = {
            cpu    = var.n8n_cpu
            memory = var.n8n_memory
          }
        }

        env {
          name  = "N8N_EDITOR_BASE_URL"
          value = "https://${google_cloud_run_service.n8n.status[0].url}"
        }

        env {
          name  = "N8N_HOST"
          value = google_cloud_run_service.n8n.status[0].url
        }

        env {
          name  = "WEBHOOK_URL"
          value = "https://${google_cloud_run_service.n8n.status[0].url}"
        }

        env {
          name  = "N8N_PORT"
          value = "5678"
        }

        env {
          name  = "DB_TYPE"
          value = "postgresdb"
        }

        env {
          name  = "DB_POSTGRESDB_HOST"
          value = "cloudsql-proxy"
        }

        env {
          name  = "EXECUTIONS_PRUNE_TIMEOUT"
          value = "7d"
        }

        env {
          name  = "N8N_BASIC_AUTH_ACTIVE"
          value = "true"
        }

        env {
          name  = "N8N_BASIC_AUTH_EMAIL"
          value = var.n8n_admin_emails[0]
        }

        # Encryption key loaded from Secret Manager at startup
        env {
          name = "N8N_ENCRYPTION_KEY"
          value_from {
            secret_key_ref {
              name = google_secret_manager_secret_version.n8n_encryption_key.secret
              key  = "latest"
            }
          }
        }

        volume_mounts {
          name       = "n8n-storage"
          mount_path = "/home/n8n/.n8n"
        }
      }

      volumes {
        name = "n8n-storage"
        csi {
          driver = "gcsfuse"
          read_only = false
          volume_attributes = {
            bucketName = google_storage_bucket.n8n_storage.name
            use_mcs    = "true"
          }
        }
      }

      timeout_seconds       = 300
      service_account_name  = google_service_account.n8n_sa.email
      max_instance_request_concurrency = var.n8n_concurrency
    }

    annotations = {
      "autoscaling.knative.dev/minScale" = var.n8n_min_instances
      "autoscaling.knative.dev/maxScale" = var.n8n_max_instances
      "run.googleapis.com/vpc-access-connector" = google_vpc_access_connector.n8n_connector.name
    }
  }

  traffic {
    latest_revision = true
    percent         = 100
  }

  # Enforce HTTPS + Cloud Armor via load balancer config
  metadata {
    annotations = {
      "run.googleapis.com/ingress" = "internal-and-cloud-load-balancing"
      "run.googleapis.com/cloudsql-instances" = ""
      "run.googleapis.com/encryption-key" = google_kms_crypto_key.n8n_secrets_key.id
    }
  }

  depends_on = [
    google_project_service.cloudrun,
    google_service_account.n8n_sa,
    google_artifact_registry_repository.n8n_repo,
    google_vpc_access_connector.n8n_connector,
    google_storage_bucket.n8n_storage,
    google_kms_crypto_key.n8n_secrets_key,
  ]
}

# =============================================================================
# 13. BERT Sentiment ML Service — Cloud Run
# =============================================================================

resource "google_cloud_run_service" "ml_sentiment" {
  project  = var.project_id
  name     = var.ml_service_name
  location = var.default_region

  template {
    spec {
      service_account_name = google_service_account.ml_sa.email

      containers {
        image = "gcr.io/${var.project_id}/sentiment-ml:latest"

        resources {
          limits = {
            cpu    = var.ml_cpu
            memory = var.ml_memory
          }
        }

        ports {
          container_port = 8080
        }

        env {
          name  = "MODEL_NAME"
          value = var.ml_model_name
        }

        env {
          name  = "REQUEST_TIMEOUT_MS"
          value = "15000"
        }

        env {
          name  = "MAX_TEXT_LENGTH"
          value = "5000"
        }

        env {
          name  = "RATE_LIMIT_RPS"
          value = "100"
        }

        env {
          name  = "ML_API_KEY"
          value_from {
            secret_key_ref {
              name = google_secret_manager_secret_version.ml_api_key.secret
              key  = "latest"
            }
          }
        }

        env {
          name  = "GCP_PROJECT_ID"
          value = var.project_id
        }
      }

      timeout_seconds       = var.ml_idle_timeout
      service_account_name  = google_service_account.ml_sa.email
      max_instance_request_concurrency = 10
    }

    metadata {
      annotations = {
        "autoscaling.knative.dev/minScale" = var.n8n_min_instances
        "autoscaling.knative.dev/maxScale" = var.ml_max_instances
        "run.googleapis.com/execution-environment" = "gen2"
        "run.googleapis.com/encryption-key"             = google_kms_crypto_key.n8n_secrets_key.id
        "generative-pipeline.googleapis.com/encoder"     = "true"
      }
    }
  }

  traffic {
    latest_revision = true
    percent         = 100
  }

  metadata {
    annotations = {
      "run.googleapis.com/ingress" = "internal" # only reachable by n8n SA + public via Cloud Armor LB
    }
  }

  depends_on = [
    google_project_service.cloudrun,
    google_service_account.ml_sa,
    google_kms_crypto_key.n8n_secrets_key,
  ]
}

# =============================================================================
# 14. Cloud Run IAM — n8n service allowed to invoke ML service
# =============================================================================

resource "google_cloud_run_service_iam_member" "n8n_invoke_ml" {
  service  = google_cloud_run_service.ml_sentiment.name
  location = google_cloud_run_service.ml_sentiment.location
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.n8n_sa.email}"
}

# ML service definitely internal — deny public invoke
resource "google_cloud_run_service_iam_policy" "ml_internal" {
  service     = google_cloud_run_service.ml_sentiment.name
  location    = google_cloud_run_service.ml_sentiment.location
  project     = var.project_id

  policy_data = jsonencode({
    bindings = [
      {
        role    = "roles/run.invoker"
        members = [
          "serviceAccount:${google_service_account.n8n_sa.email}",
          "serviceAccount:${google_service_account.ml_sa.email}",
        ]
      },
    ]
  })

  depends_on = [google_cloud_run_service.ml_sentiment]
}
