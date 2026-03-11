# ==============================================================================
# modules/vpc_connector/variables.tf
# ==============================================================================
# Variables pour créer un Serverless VPC Access Connector
# (utilisé par Cloud Run pour accéder à des ressources privées)
# ==============================================================================

variable "project_id" {
  description = "ID du projet GCP"
  type        = string
}

variable "region" {
  description = "Région (ex: europe-west1)"
  type        = string
}

variable "name" {
  description = "Nom du VPC Connector (ex: speedline-dev-connector)"
  type        = string
}

variable "network" {
  description = "Nom du réseau VPC (ex: default ou custom-vpc)"
  type        = string
  default     = "default"
}

variable "ip_cidr_range" {
  description = "Plage CIDR réservée au connector (ex: 10.8.0.0/28)"
  type        = string
  default     = "10.8.0.0/28"
}

variable "min_instances" {
  description = "Min instances du connector (0 en dev pour réduire le coût)"
  type        = number
  default     = 2
}

variable "max_instances" {
  description = "Max instances du connector"
  type        = number
  default     = 3
}

variable "machine_type" {
  description = "Machine type du connector (e2-micro, e2-small, ...)"
  type        = string
  default     = "e2-micro"
}

variable "labels" {
  description = "Labels GCP"
  type        = map(string)
  default     = {}
}