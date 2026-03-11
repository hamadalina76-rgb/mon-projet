# ------------------------------------------------------------------
# OUTPUTS DU MODULE
# ------------------------------------------------------------------
# Les outputs permettent :
# - De récupérer des informations après terraform apply
# - D'alimenter d'autres modules
# - D'être utilisés dans CI/CD
# ------------------------------------------------------------------

# Nom complet interne du repository
output "repository_name" {
  description = "Nom complet du repository"
  value       = google_artifact_registry_repository.docker_repo.name
}

# Région du repository
output "repository_location" {
  description = "Région du repository"
  value       = google_artifact_registry_repository.docker_repo.location
}

# URL Docker complète pour push/pull
# Format :
# europe-west1-docker.pkg.dev/PROJECT_ID/REPOSITORY_ID
output "docker_repo_url" {
  description = "URL Docker à utiliser pour docker tag et docker push"
  value       = "${google_artifact_registry_repository.docker_repo.location}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.docker_repo.repository_id}"
}