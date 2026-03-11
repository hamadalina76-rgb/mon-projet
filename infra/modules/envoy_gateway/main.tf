resource "google_cloud_run_v2_service" "envoy" {
  name     = var.name
  location = var.region
  project  = var.project_id

  labels = var.labels

  template {
    service_account = var.service_account_email

    containers {
      image = var.image

      ports {
        container_port = 8080
      }

      env {
        name  = "UPSTREAM_URL"
        value = var.upstream_url
      }

      # Optionnel: tuning
      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }
    }
  }

  traffic {
    percent = 100
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
  }
}

# ✅ Public access si allow_unauthenticated = true
resource "google_cloud_run_v2_service_iam_member" "public" {
  count    = var.allow_unauthenticated ? 1 : 0
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.envoy.name

  role   = "roles/run.invoker"
  member = "allUsers"
}