# ==============================================================================
# modules/vpc_connector/outputs.tf
# ==============================================================================
# Sorties utiles pour attacher le connector à Cloud Run
# ==============================================================================

output "name" {
  description = "Nom du VPC Connector"
  value       = google_vpc_access_connector.this.name
}

output "id" {
  description = "ID complet du VPC Connector"
  value       = google_vpc_access_connector.this.id
}