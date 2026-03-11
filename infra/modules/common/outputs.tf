# ==============================================================================
# OUTPUTS - MODULE COMMON
# ==============================================================================

output "enabled_apis" {
  description = "Liste des APIs activées"
  value       = [for api in google_project_service.apis : api.service]
}

output "apis_ready" {
  description = "Indicateur que les APIs sont prêtes (après délai)"
  value       = time_sleep.wait_for_apis.id
}

