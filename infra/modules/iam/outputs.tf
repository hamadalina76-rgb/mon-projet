output "gitlab_ci_sa_email" {
  description = "Email du Service Account GitLab CI"
  value       = google_service_account.gitlab_ci.email
}

output "cloudrun_runtime_sa_email" {
  description = "Email du Service Account Cloud Run runtime"
  value       = google_service_account.cloudrun_runtime.email
}