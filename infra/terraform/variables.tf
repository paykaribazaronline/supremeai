variable "project_id" {
  description = "GCP Project ID where all resources will be provisioned."
  type        = string
}

variable "default_region" {
  description = "Default GCP region for all resources."
  type        = string
  default     = "us-central1"
}

variable "environment" {
  description = "Environment label (production, staging, dev)."
  type        = string
  default     = "production"
}

# n8n on Cloud Run
variable "n8n_service_name" {
  type    = string
  default = "n8n-workflow"
}

variable "n8n_cpu" {
  type    = string
  default = "1"
}

variable "n8n_memory" {
  type    = string
  default = "2Gi"
}

variable "n8n_max_instances" {
  type    = number
  default = 10
}

variable "n8n_min_instances" {
  type    = number
  default = 0
}

variable "n8n_concurrency" {
  type    = number
  default = 80
}

variable "n8n_admin_emails" {
  description = "List of admin email addresses allowed to access n8n."
  type        = list(string)
  default     = ["admin@supremeai.dev"]
}

# ML Sentiment Service
variable "ml_service_name" {
  type    = string
  default = "sentiment-ml-service"
}

variable "ml_model_name" {
  type    = string
  default = "distilbert-base-uncased-finetuned-sst-2-english"
}

variable "ml_cpu" {
  type    = string
  default = "2"
}

variable "ml_memory" {
  type    = string
  default = "4Gi"
}

variable "ml_max_instances" {
  type    = number
  default = 20
}

variable "ml_idle_timeout" {
  type    = number
  default = 300
}

# Named Network for VPC Connector
variable "vpc_connector_name" {
  type    = string
  default = "n8n-vpc-connector"
}

variable "vpc_connector_ips" {
  type        = number
  default     = 2
  description = "Number of IP addresses in VPC connector range."
}

# GCS
variable "n8n_bucket_name" {
  type    = string
  default = "supremeai-n8n-storage-prod"
}

variable "n8n_backup_bucket_name" {
  type    = string
  default = "supremeai-n8n-backups-prod"
}

# Secrets
variable "n8n_encryption_key_secret_name" {
  type    = string
  default = "n8n-encryption-key"
}

variable "n8n_basic_auth_secret_name" {
  type    = string
  default = "n8n-basic-auth-credentials"
}

variable "ml_api_key_secret_name" {
  type    = string
  default = "ml-api-gateway-key"
}

# Cloud Armor
variable "cloud_armor_allowed_admin_cidrs" {
  description = "CIDR blocks allowed to reach n8n admin (internal IPs only, leave empty for none)."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# Monitoring
variable "alert_email" {
  type    = string
  default = "alerts@supremeai.dev"
}
