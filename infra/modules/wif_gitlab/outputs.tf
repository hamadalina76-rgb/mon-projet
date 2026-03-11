output "workload_identity_provider" {
  description = "Resource name du provider WIF"
  value       = google_iam_workload_identity_pool_provider.provider.name
}

output "workload_identity_pool" {
  description = "Resource name du pool WIF"
  value       = google_iam_workload_identity_pool.pool.name
}
