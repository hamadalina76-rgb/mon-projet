# ==============================================================================
# infra/modules/redis/variables.tf
# ==============================================================================
# Module Memorystore Redis (cache)
# - Crée 1 instance Redis BASIC (pas HA) pour réduire le coût
# - Connexion via IP privée (dans un VPC)
# - Cloud Run accède via Serverless VPC Access Connector (recommandé)
# ==============================================================================

variable "project_id" {
  description = "Projet GCP qui héberge l'instance Redis"
  type        = string
}

variable "region" {
  description = "Région (ex: europe-west1)"
  type        = string
}

variable "name" {
  description = "Nom de l'instance Redis"
  type        = string
}

variable "memory_size_gb" {
  description = "Taille mémoire (GB). Minimum 1GB pour BASIC."
  type        = number
  default     = 1
}

variable "redis_version" {
  description = "Version Redis (ex: REDIS_6_X, REDIS_7_0)"
  type        = string
  default     = "REDIS_7_0"
}

variable "tier" {
  description = "BASIC (moins cher) ou STANDARD_HA"
  type        = string
  default     = "BASIC"
}

variable "network" {
  description = "Self-link du VPC (ex: projects/<proj>/global/networks/default)"
  type        = string
  default     = "default"
}

variable "labels" {
  description = "Labels GCP"
  type        = map(string)
  default     = {}
}