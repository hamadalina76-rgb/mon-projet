# ------------------------------------------------------------------
# VERSIONS & PROVIDER CONSTRAINTS
# ------------------------------------------------------------------
# Ce fichier définit :
# - La version minimale de Terraform
# - La version du provider Google utilisée
#
# Pourquoi ?
# → Garantir la compatibilité entre environnements
# → Éviter les erreurs liées aux mises à jour automatiques
# → Assurer la reproductibilité (Dev / Staging / Prod)
# ------------------------------------------------------------------

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.30"
    }
  }
}