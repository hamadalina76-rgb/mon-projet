variable "project_id" {
  type        = string
  description = "ID du projet GCP"
}

variable "environment" {
  type        = string
  description = "Environnement (dev/staging/prod) utilisé dans le naming"
}

variable "labels" {
  type        = map(string)
  description = "Labels communs (optionnel)"
  default     = {}
}