# ==============================================================================
# modules/pubsub/outputs.tf
# ==============================================================================
output "dlq_topic" {
  value       = google_pubsub_topic.dlq.name
  description = "Nom du DLQ topic"
}

output "topics" {
  value       = { for k, v in google_pubsub_topic.topics : k => v.name }
  description = "Topics créés"
}

output "subscriptions" {
  value       = { for k, v in google_pubsub_subscription.subs : k => v.name }
  description = "Subscriptions créées"
}