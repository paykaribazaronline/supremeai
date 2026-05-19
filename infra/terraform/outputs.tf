output "n8n_public_url" {
  description = "Public Cloud Run URL for the n8n workflow orchestrator (the primary API endpoint)."
  value       = "https://${google_cloud_run_service.n8n.status[0].url}"
}

output "n8n_service_url" {
  description = "Cloud Run service URL (plain string)."
  value       = google_cloud_run_service.n8n.status[0].url
}

output "ml_service_url" {
  description = "Cloud Run URL for the BERT sentiment ML inference service."
  value       = "https://${google_cloud_run_service.ml_sentiment.status[0].url}"
}

output "n8n_bucket" {
  description = "GCS bucket for n8n persistent workflow storage."
  value       = google_storage_bucket.n8n_storage.name
}

output "n8n_backup_bucket" {
  description = "GCS bucket for n8n automated backups."
  value       = google_storage_bucket.n8n_backup.name
}

output "n8n_service_account" {
  description = "n8n Cloud Run service account email."
  value       = google_service_account.n8n_sa.email
}

output "ml_service_account" {
  description = "ML inference service account email."
  value       = google_service_account.ml_sa.email
}

output "cloud_armor_policy" {
  description = "Cloud Armor security policy name."
  value       = google_compute_security_policy.n8n_armor.name
}

output "vpc_connector" {
  description = "VPC connector name for private connectivity."
  value       = google_vpc_access_connector.n8n_connector.name
}

output "n8n_secret_version" {
  description = "Latest published version of n8n encryption secret."
  value       = google_secret_manager_secret_version.n8n_encryption_key.version_id
}
