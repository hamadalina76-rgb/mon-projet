# ==============================================================================
# infra/modules/cloudsql/outputs.tf
# ==============================================================================

output "instance_name" {
  value       = google_sql_database_instance.this.name
  description = "Nom de l'instance Cloud SQL"
}

output "instance_connection_name" {
  value       = google_sql_database_instance.this.connection_name
  description = "Connection name (project:region:instance) pour Cloud SQL Connector"
}

output "public_ip_address" {
  value       = try(google_sql_database_instance.this.public_ip_address, null)
  description = "IP publique (si enable_public_ip=true)"
}

output "databases" {
  value       = [for d in google_sql_database.db : d.name]
  description = "Liste des DB créées"
}

output "users" {
  value       = { for k, u in google_sql_user.users : k => u.name }
  description = "Users créés (map key => username)"
}

