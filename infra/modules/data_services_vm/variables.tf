variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
}

variable "name" {
  description = "Compute Engine VM name"
  type        = string
}

variable "zone" {
  description = "Compute Engine zone"
  type        = string
}

variable "machine_type" {
  description = "Compute Engine machine type"
  type        = string
  default     = "e2-medium"
}

variable "network" {
  description = "VPC network name or self_link"
  type        = string
  default     = "default"
}

variable "subnetwork" {
  description = "Optional VPC subnetwork name or self_link"
  type        = string
  default     = ""
}

variable "boot_image" {
  description = "Boot image for the VM"
  type        = string
  default     = "debian-cloud/debian-12"
}

variable "boot_disk_size_gb" {
  description = "Boot disk size in GB"
  type        = number
  default     = 40
}

variable "boot_disk_type" {
  description = "Boot disk type"
  type        = string
  default     = "pd-balanced"
}

variable "service_account_email" {
  description = "Optional service account email attached to VM"
  type        = string
  default     = ""
}

variable "service_account_scopes" {
  description = "Scopes for VM service account"
  type        = list(string)
  default = [
    "https://www.googleapis.com/auth/logging.write",
    "https://www.googleapis.com/auth/monitoring.write"
  ]
}

variable "data_ports" {
  description = "Data service ports exposed internally"
  type        = list(string)
  default     = ["6379", "8123", "9000"]
}

variable "allowed_source_ranges" {
  description = "CIDR ranges allowed to access data ports"
  type        = list(string)
}

variable "enable_iap_ssh" {
  description = "Whether to allow SSH from Google IAP range"
  type        = bool
  default     = true
}

variable "admin_ssh_source_ranges" {
  description = "Optional direct admin SSH source ranges"
  type        = list(string)
  default     = []
}

variable "labels" {
  description = "Labels to apply to resources"
  type        = map(string)
  default     = {}
}

variable "startup_script" {
  description = "Optional startup script"
  type        = string
  default     = ""
}

variable "network_tags" {
  description = "Additional network tags"
  type        = list(string)
  default     = []
}
