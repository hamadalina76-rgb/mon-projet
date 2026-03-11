# ==============================================================================
# infra/modules/cloudsql/variables.tf
# ==============================================================================

variable "project_id" {
  description = "Projet GCP qui héberge l'instance Cloud SQL"
  type        = string
}

variable "region" {
  description = "Région Cloud SQL (ex: europe-west1)"
  type        = string
}

variable "instance_name" {
  description = "Nom de l'instance Cloud SQL"
  type        = string
}

variable "database_version" {
  description = "Version PostgreSQL Cloud SQL"
  type        = string
  default     = "POSTGRES_15"
}

variable "tier" {
  description = "Machine type (ex: db-custom-1-3840)"
  type        = string
  default     = "db-custom-1-3840"
}

variable "disk_size_gb" {
  description = "Taille disque en GB"
  type        = number
  default     = 10
}

variable "disk_type" {
  description = "Type disque (PD_SSD recommandé)"
  type        = string
  default     = "PD_SSD"
}

variable "availability_type" {
  description = "ZONAL (moins cher) ou REGIONAL (HA)"
  type        = string
  default     = "ZONAL"
}

variable "deletion_protection" {
  description = "Protection suppression (prod: true)"
  type        = bool
  default     = false
}

variable "databases" {
  description = "Liste des DB à créer dans la même instance"
  type        = list(string)
}

variable "users" {
  description = "Users à créer (dev/staging/prod) + DB associée pour organisation"
  type = map(object({
    name     = string
    password = string
    database = string
  }))
}

variable "enable_public_ip" {
  description = "Activer IPv4 publique (simplifie cross-project)"
  type        = bool
  default     = true
}

variable "authorized_networks" {
  description = "Réseaux autorisés si public IP (peut rester vide avec Connector)"
  type = list(object({
    name  = string
    value = string
  }))
  default = []
}

variable "backup_enabled" {
  description = "Activer backups automatiques"
  type        = bool
  default     = false
}

variable "labels" {
  description = "Labels GCP"
  type        = map(string)
  default     = {}
}

