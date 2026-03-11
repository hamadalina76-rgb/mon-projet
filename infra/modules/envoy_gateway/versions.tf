# ------------------------------------------------------------
# Terraform & Provider Versions — Envoy Gateway Module
# ------------------------------------------------------------
# Ce fichier verrouille :
# - La version minimale de Terraform
# - La version du provider Google
#
# Objectif :
# - Assurer la stabilité
# - Éviter les breaking changes
# - Garantir la compatibilité CI/CD
# ------------------------------------------------------------

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.30"
    }
  }
}