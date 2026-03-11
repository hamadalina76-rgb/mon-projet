output "atlas_project_id" {
  description = "MongoDB Atlas project ID"
  value       = var.atlas_project_id
}

output "cluster_name" {
  description = "MongoDB Atlas cluster name"
  value       = var.atlas_cluster_name
}

output "atlas_region" {
  description = "MongoDB Atlas region"
  value       = var.atlas_region
}

output "mongo_db_name" {
  description = "MongoDB database name"
  value       = var.mongo_db_name
}

output "mongo_db_username" {
  description = "MongoDB database username"
  value       = var.mongo_db_username
}

output "mongo_allowed_cidr" {
  description = "MongoDB Atlas access-list CIDR"
  value       = var.mongo_allowed_cidr
}

output "mongodb_uri" {
  description = "MongoDB URI (sensitive)"
  value       = var.mongodb_uri
  sensitive   = true
}

output "manual_mode" {
  description = "True when Atlas is managed manually outside Terraform"
  value       = var.manage_resources == false
}
