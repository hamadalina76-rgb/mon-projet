variable "project_id" { type = string }
variable "environment" { type = string }

variable "gitlab_project_path" {
  type        = string
  description = "Ex: Mohammed_KHNine/speedline"
}

variable "service_account_email" {
  type        = string
  description = "Service account à impersonner depuis GitLab"
}

variable "ref_prefix" {
  type        = string
  description = "Prefix Git tag autorisé (ex: dev-)"
  default     = "dev-"
}

variable "inject_cloud_run_port" {
  description = "Injecte JAVA_TOOL_OPTIONS=-Dserver.port=<port> pour forcer Spring Boot à écouter sur le port Cloud Run"
  type        = bool
  default     = true
}