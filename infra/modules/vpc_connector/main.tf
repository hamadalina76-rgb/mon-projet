# ==============================================================================
# modules/vpc_connector/main.tf
# ==============================================================================
# Serverless VPC Access Connector
# Permet à Cloud Run d'accéder aux ressources privées
# (ex: Memorystore Redis en IP privée)
#
# IMPORTANT :
# La ressource google_vpc_access_connector NE supporte PAS les labels.
# ==============================================================================

resource "google_vpc_access_connector" "this" {
  project = var.project_id
  region  = var.region
  name    = var.name

  network       = var.network
  ip_cidr_range = var.ip_cidr_range

  min_instances = var.min_instances
  max_instances = var.max_instances
  machine_type  = var.machine_type
}