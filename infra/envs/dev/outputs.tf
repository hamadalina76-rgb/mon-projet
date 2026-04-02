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

output "notification_warmup_schedule" {
  description = "Cron schedule currently used by notification-service warm-up job (DEV)"
  value       = var.notification_warmup_schedule
}

output "notification_push_subscription_enabled" {
  description = "Whether optional Pub/Sub push prep subscription is enabled in DEV"
  value       = var.notification_push_subscription_enabled
}

output "notification_push_subscription_name" {
  description = "Name of optional Pub/Sub push prep subscription in DEV (empty when disabled)"
  value       = var.notification_push_subscription_enabled ? google_pubsub_subscription.notification_push_prep_dev[0].name : ""
}

output "data_services_vm_enabled" {
  description = "Whether data-services VM is enabled in DEV"
  value       = var.enable_data_services_vm
}

output "data_services_vm_name" {
  description = "Name of the optional data-services VM in DEV"
  value       = var.enable_data_services_vm ? module.data_services_vm[0].name : ""
}

output "data_services_vm_zone" {
  description = "Zone of the optional data-services VM in DEV"
  value       = var.enable_data_services_vm ? module.data_services_vm[0].zone : ""
}

output "data_services_vm_project_id" {
  description = "Project ID hosting the optional data-services VM in DEV"
  value       = var.enable_data_services_vm ? var.project_id : ""
}

output "data_services_vm_private_ip" {
  description = "Private IP of optional data-services VM in DEV"
  value       = var.enable_data_services_vm ? module.data_services_vm[0].private_ip : ""
}

output "data_services_redis_endpoint" {
  description = "Redis endpoint for the data-services VM (host:port)"
  value       = var.enable_data_services_vm ? "${module.data_services_vm[0].private_ip}:6379" : ""
}

output "data_services_clickhouse_http_endpoint" {
  description = "ClickHouse HTTP endpoint for the data-services VM"
  value       = var.enable_data_services_vm ? "http://${module.data_services_vm[0].private_ip}:8123" : ""
}
