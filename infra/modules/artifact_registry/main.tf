# ------------------------------------------------------------------
# MODULE : ARTIFACT REGISTRY (DOCKER)
# ------------------------------------------------------------------
# Objectif :
# Créer un repository Docker dans Google Artifact Registry.
#
# Ce repository servira à :
# - Stocker les images Docker des microservices
# - Permettre à GitLab CI/CD de pousser les images
# - Permettre à Cloud Run de récupérer (pull) les images
#
# Architecture :
# GitLab CI → Build → Push → Artifact Registry
# Cloud Run → Pull image → Deploy service
# ------------------------------------------------------------------

resource "google_artifact_registry_repository" "docker_repo" {

  # Projet GCP cible
  project = var.project_id

  # Région du repository
  location = var.region

  # Nom du repository (ex: speedline-docker)
  repository_id = var.repository_id

  # Description administrative
  description = "Docker images repository for Speedline microservices"

  # Format du repository
  # Ici DOCKER (car nous stockons des images Docker)
  format = "DOCKER"

  # Labels pour organisation & gouvernance
  labels = var.labels
}