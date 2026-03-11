# ------------------------------------------------------------------
# VARIABLES D'ENTRÉE DU MODULE
# ------------------------------------------------------------------
# Ce module est réutilisable.
# Il ne contient aucune valeur fixe.
# Toutes les valeurs viennent de l'environnement (dev/staging/prod).
# ------------------------------------------------------------------

# ID du projet Google Cloud
variable "project_id" {
  description = "ID du projet GCP dans lequel créer le repository"
  type        = string
}

# Région GCP (ex: europe-west1)
variable "region" {
  description = "Région où sera créé le repository Artifact Registry"
  type        = string
}

# Nom du repository Docker
variable "repository_id" {
  description = "Nom du dépôt Docker dans Artifact Registry"
  type        = string
}

# Labels pour gouvernance & organisation
variable "labels" {
  description = "Labels GCP appliqués au repository"
  type        = map(string)
  default     = {}
}