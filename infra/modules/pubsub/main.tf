# ==============================================================================
# MODULE PUBSUB (IDE friendly)
# - Topics
# - DLQ topic
# - Subscriptions + DLQ policy + retry policy
# - IAM publisher (topics + dlq)
# - IAM subscriber (subscriptions)
# ==============================================================================

data "google_project" "this" {
  project_id = var.project_id
}

# ------------------------------------------------------------------------------
# TOPICS
# ------------------------------------------------------------------------------
resource "google_pubsub_topic" "topics" {
  for_each = toset(var.topics)

  project = var.project_id
  name    = each.value
  labels  = var.labels
}

# ------------------------------------------------------------------------------
# DLQ TOPIC
# ------------------------------------------------------------------------------
resource "google_pubsub_topic" "dlq" {
  project = var.project_id
  name    = var.dlq_topic
  labels  = var.labels
}

# Pub/Sub service agent doit pouvoir publier dans le DLQ
resource "google_pubsub_topic_iam_member" "dlq_pubsub_service_publisher" {
  project = var.project_id
  topic   = google_pubsub_topic.dlq.id
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:service-${data.google_project.this.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

# ------------------------------------------------------------------------------
# SUBSCRIPTIONS
# ------------------------------------------------------------------------------
resource "google_pubsub_subscription" "subs" {
  for_each = var.subscriptions

  project              = var.project_id
  name                 = each.key
  topic                = google_pubsub_topic.topics[each.value.topic].id
  ack_deadline_seconds = each.value.ack_deadline_seconds

  message_retention_duration = "604800s"
  retain_acked_messages      = false
  labels                     = var.labels

  retry_policy {
    minimum_backoff = each.value.minimum_backoff
    maximum_backoff = each.value.maximum_backoff
  }

  dead_letter_policy {
    dead_letter_topic     = google_pubsub_topic.dlq.id
    max_delivery_attempts = each.value.max_delivery_attempts
  }
}

# ------------------------------------------------------------------------------
# IAM PUBLISHER sur TOPICS
# Ici on crée une map "clé => topic_name" puis une map "clé => sa_email"
# => plus d'objets each.value.topic/sa -> IDE content
# ------------------------------------------------------------------------------
locals {
  publisher_topic_by_key = {
    for pair in setproduct(var.topics, var.publisher_service_accounts) :
    "${pair[0]}|${pair[1]}" => pair[0]
  }

  publisher_sa_by_key = {
    for pair in setproduct(var.topics, var.publisher_service_accounts) :
    "${pair[0]}|${pair[1]}" => pair[1]
  }
}

resource "google_pubsub_topic_iam_member" "publisher" {
  for_each = local.publisher_topic_by_key

  project = var.project_id
  topic   = google_pubsub_topic.topics[each.value].id
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${local.publisher_sa_by_key[each.key]}"
}

# Autoriser aussi publier vers DLQ (optionnel)
resource "google_pubsub_topic_iam_member" "dlq_publisher" {
  for_each = toset(var.publisher_service_accounts)

  project = var.project_id
  topic   = google_pubsub_topic.dlq.id
  role    = "roles/pubsub.publisher"
  member  = "serviceAccount:${each.value}"
}

# ------------------------------------------------------------------------------
# IAM SUBSCRIBER sur SUBSCRIPTIONS
# Même idée : map clé => subscription_name et map clé => sa_email
# ------------------------------------------------------------------------------
locals {
  subscriber_sub_by_key = {
    for pair in setproduct(keys(var.subscriptions), var.subscriber_service_accounts) :
    "${pair[0]}|${pair[1]}" => pair[0]
  }

  subscriber_sa_by_key = {
    for pair in setproduct(keys(var.subscriptions), var.subscriber_service_accounts) :
    "${pair[0]}|${pair[1]}" => pair[1]
  }
}

resource "google_pubsub_subscription_iam_member" "subscriber" {
  for_each = local.subscriber_sub_by_key

  project      = var.project_id
  subscription = google_pubsub_subscription.subs[each.value].name
  role         = "roles/pubsub.subscriber"
  member       = "serviceAccount:${local.subscriber_sa_by_key[each.key]}"
}