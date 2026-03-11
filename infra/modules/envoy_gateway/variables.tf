variable "project_id" {
  type        = string
  description = "ID du projet GCP"
}

variable "region" {
  type        = string
  description = "Région GCP"
}

variable "environment" {
  type        = string
  description = "Nom env (dev/staging/prod)"
}

variable "name" {
  type        = string
  description = "Nom du service Envoy Gateway (Cloud Run)"
  default     = "envoy-gateway"
}

variable "image" {
  type        = string
  description = "Image docker Envoy (Artifact Registry)"
}

variable "allow_unauthenticated" {
  type        = bool
  description = "Envoy public ou non"
  default     = true
}

variable "service_account_email" {
  type        = string
  description = "Service account runtime pour Envoy"
}

variable "upstream_url" {
  type        = string
  description = "URL interne (Cloud Run) du backend à protéger (api-gateway)"
}

variable "labels" {
  type        = map(string)
  description = "Labels"
  default     = {}
}