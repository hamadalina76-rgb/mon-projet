# ==============================================================================
# MODULE COMMON : Activation des APIs GCP
# ==============================================================================
# Active toutes les APIs nécessaires pour le projet Speedline
# ==============================================================================

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.30"
    }
    time = {
      source  = "hashicorp/time"
      version = "~> 0.9.0"
    }
  }
}

# ------------------------------------------------------------------------------
# Activation des APIs GCP requises
# ------------------------------------------------------------------------------

locals {
  # Liste complète des APIs nécessaires
  apis = [
    "cloudresourcemanager.googleapis.com", # Gestion des ressources
    "iam.googleapis.com",                  # IAM & Service Accounts
    "compute.googleapis.com",              # Compute Engine (pour VPC)
    "run.googleapis.com",                  # Cloud Run
    "cloudbuild.googleapis.com",           # Cloud Build (frontend image builds)
    "artifactregistry.googleapis.com",     # Artifact Registry
    "storage.googleapis.com",              # Cloud Storage
    "sqladmin.googleapis.com",             # Cloud SQL
    "pubsub.googleapis.com",               # Pub/Sub
    "redis.googleapis.com",                # Memorystore Redis
    "vpcaccess.googleapis.com",            # Serverless VPC Access
    "servicenetworking.googleapis.com",    # Service Networking
    "cloudapis.googleapis.com",            # Cloud APIs
    "logging.googleapis.com",              # Cloud Logging
    "monitoring.googleapis.com",           # Cloud Monitoring
    "secretmanager.googleapis.com",        # Secret Manager
    "iamcredentials.googleapis.com",       # IAM Credentials (pour WIF)
    "sts.googleapis.com",                  # Security Token Service (pour WIF)
    "cloudscheduler.googleapis.com",       # Cloud Scheduler (warm-up pings)
  ]
}

resource "google_project_service" "apis" {
  for_each = toset(local.apis)

  project = var.project_id
  service = each.value

  # Ne pas désactiver les APIs lors de la destruction (sécurité)
  disable_on_destroy = false

  # Désactiver les services dépendants automatiquement
  disable_dependent_services = false
}

# ------------------------------------------------------------------------------
# Attente pour la propagation des APIs (évite les erreurs 403)
# ------------------------------------------------------------------------------

resource "time_sleep" "wait_for_apis" {
  depends_on = [google_project_service.apis]

  create_duration = "60s" # Attend 60 secondes après activation
}

