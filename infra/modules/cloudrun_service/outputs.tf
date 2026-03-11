# ==============================================================================
# modules/cloudrun_service/outputs.tf
# ==============================================================================
# Expose l'URL du service Cloud Run (utile pour config-server, etc.)
# ==============================================================================
output "name" {
  description = "Nom du service Cloud Run"
  value       = google_cloud_run_v2_service.this.name
}

output "uri" {
  description = "URL (URI) du service Cloud Run"
  value       = google_cloud_run_v2_service.this.uri
}

output "service_url" {
  description = "URL du service Cloud Run"
  value       = google_cloud_run_v2_service.this.uri
}