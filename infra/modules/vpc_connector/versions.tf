# ==============================================================================
# modules/vpc_connector/versions.tf
# ==============================================================================
# Déclare les versions requises du provider Google
# Compatible avec ton environnement (>= 5.30)
# ==============================================================================

terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 5.30"
    }
  }
}