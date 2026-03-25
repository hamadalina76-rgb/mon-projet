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

variable "enable_project_iam_bindings" {
  type        = bool
  description = "Active la création des google_project_iam_member. À laisser false en CI standard pour éviter les erreurs 403 setIamPolicy."
  default     = false
}