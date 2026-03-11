variable "manage_resources" {
  description = "Enable Terraform-managed Atlas resources. Keep false for M0 manual-managed mode."
  type        = bool
  default     = false
}

variable "atlas_public_key" {
  description = "MongoDB Atlas API public key"
  type        = string
  sensitive   = true
  default     = ""
}

variable "atlas_private_key" {
  description = "MongoDB Atlas API private key"
  type        = string
  sensitive   = true
  default     = ""
}

variable "atlas_org_id" {
  description = "MongoDB Atlas organization ID"
  type        = string
  default     = ""
}

variable "atlas_project_id" {
  description = "MongoDB Atlas project ID"
  type        = string
}

variable "atlas_cluster_name" {
  description = "MongoDB Atlas cluster name"
  type        = string
}

variable "atlas_region" {
  description = "MongoDB Atlas region"
  type        = string
  default     = "EU_WEST_1"
}

variable "mongo_db_name" {
  description = "MongoDB database name"
  type        = string
  default     = "speedline_notification"
}

variable "mongo_db_username" {
  description = "MongoDB Atlas database username"
  type        = string
  default     = ""
}

variable "mongo_db_password" {
  description = "MongoDB Atlas database password"
  type        = string
  sensitive   = true
  default     = ""
}

variable "mongo_allowed_cidr" {
  description = "MongoDB Atlas access-list CIDR"
  type        = string
  default     = "0.0.0.0/0"
}

variable "mongodb_uri" {
  description = "MongoDB connection URI used by workloads"
  type        = string
  sensitive   = true
}
