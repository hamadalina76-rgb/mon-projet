# ==============================================================================
# infra/modules/redis/main.tf
# ==============================================================================
# Memorystore for Redis (cache)
# - BASIC 1GiB : coût minimal
# - IP privée dans un VPC
# - Cloud Run devra utiliser un Serverless VPC Access Connector
# ==============================================================================

resource "google_redis_instance" "this" {
  project        = var.project_id
  region         = var.region
  name           = var.name

  tier           = var.tier
  memory_size_gb = var.memory_size_gb
  redis_version  = var.redis_version

  # Réseau VPC (par défaut: "default")
  # Google attend un self_link OU "projects/<project>/global/networks/<name>"
  authorized_network = (
    contains([var.network], "default")
    ? "projects/${var.project_id}/global/networks/default"
    : var.network
  )

  labels = var.labels

  # Optionnel : configs Redis (tu peux laisser vide)
  # redis_configs = {
  #   maxmemory-policy = "allkeys-lru"
  # }
}