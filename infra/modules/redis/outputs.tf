# ==============================================================================
# infra/modules/redis/outputs.tf
# ==============================================================================

output "host" {
  description = "IP privée Redis"
  value       = google_redis_instance.this.host
}

output "port" {
  description = "Port Redis (généralement 6379)"
  value       = google_redis_instance.this.port
}

output "name" {
  description = "Nom instance Redis"
  value       = google_redis_instance.this.name
}