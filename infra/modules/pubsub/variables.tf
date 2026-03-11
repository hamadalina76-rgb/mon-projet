# ==============================================================================
# modules/pubsub/variables.tf
# ==============================================================================
# Module Pub/Sub :
# - Création des topics (dont DLQ)
# - Création des subscriptions + dead-letter-policy
# - IAM publisher/subscriber pour une liste de service accounts
# ==============================================================================

variable "project_id" {
  type        = string
  description = "GCP project id"
}

variable "topics" {
  type        = list(string)
  description = "Liste des topics à créer"
}

variable "dlq_topic" {
  type        = string
  description = "Nom du DLQ topic"
}

variable "subscriptions" {
  description = "Map des subscriptions à créer"
  type = map(object({
    topic                 = string
    ack_deadline_seconds  = number
    max_delivery_attempts = number
    minimum_backoff       = string
    maximum_backoff       = string
  }))
  default = {}
}

variable "publisher_service_accounts" {
  type        = list(string)
  description = "Emails des SA autorisés à publier sur les topics"
  default     = []
}

variable "subscriber_service_accounts" {
  type        = list(string)
  description = "Emails des SA autorisés à consommer les subscriptions"
  default     = []
}

variable "labels" {
  type        = map(string)
  description = "Labels appliqués aux ressources"
  default     = {}
}