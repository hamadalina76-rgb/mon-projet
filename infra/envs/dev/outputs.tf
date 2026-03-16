output "artifact_registry_docker_repo_url" {
  description = "URL du dépôt Docker Artifact Registry"
  value       = module.artifact_registry.docker_repo_url
}

output "gitlab_ci_sa_email" {
  description = "Email du Service Account GitLab CI"
  value       = module.iam.gitlab_ci_sa_email
}

output "cloudrun_runtime_sa_email" {
  description = "Email du Service Account Cloud Run runtime"
  value       = module.iam.cloudrun_runtime_sa_email
}

output "wif_provider_name" {
  value = module.wif_gitlab.workload_identity_provider
}

output "wif_pool_name" {
  value = module.wif_gitlab.workload_identity_pool
}

output "eureka_server_url" {
  value       = module.eureka_server.uri
  description = "URL Cloud Run Eureka Server"
}

output "config_server_url" {
  value       = module.config_server.uri
  description = "URL Cloud Run Config Server"
}

output "api_gateway_url" {
  value       = module.api_gateway.uri
  description = "URL Cloud Run API Gateway"
}

output "user_service_url" {
  value       = module.user_service.uri
  description = "URL Cloud Run User Service"
}

output "partner_service_url" {
  value       = module.partner_service.uri
  description = "URL Cloud Run Partner Service"
}

output "location_service_url" {
  value       = module.location_service.uri
  description = "URL Cloud Run Location Service"
}

output "notification_service_url" {
  value       = module.notification_service.uri
  description = "URL Cloud Run Notification Service"
}

output "partner_dashboard_url" {
  value       = module.partner_dashboard.uri
  description = "URL Cloud Run Partner Dashboard"
}

output "admin_panel_url" {
  value       = module.admin_panel.uri
  description = "URL Cloud Run Admin Panel"
}

output "mongo_atlas_project_id" {
  value       = module.mongo_atlas.atlas_project_id
  description = "MongoDB Atlas project ID used by dev env"
}

output "mongo_atlas_cluster_name" {
  value       = module.mongo_atlas.cluster_name
  description = "MongoDB Atlas cluster name used by dev env"
}