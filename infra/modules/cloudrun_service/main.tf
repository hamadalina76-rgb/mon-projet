# ==============================================================================
# infra/modules/cloudrun_service/main.tf,
# ==============================================================================
# Module générique Cloud Run v2
#
# ✅ Fix Cloud Run PORT (IMPORTANT) :
# - Cloud Run définit automatiquement la variable système PORT.
# - Il est INTERDIT de la définir nous-même (sinon erreur 400).
# - Les apps Spring Boot doivent écouter sur le port Cloud Run.
#   => On force Spring à écouter sur var.port via JAVA_TOOL_OPTIONS
#
# ✅ Stabilisation Terraform :
# - var.env_vars est une map => ordre non garanti
# - Cloud Run peut renvoyer les env vars dans un ordre différent
# - Terraform détecte des diffs faux
# => On trie les clés pour stabiliser l'ordre.
#
# ✅ Validation scaling :
# - max_instances >= min_instances (cross-variable),,
# ==============================================================================

# ------------------------------------------------------------------------------
# Validation logique scaling : max_instances >= min_instances
# ------------------------------------------------------------------------------
resource "terraform_data" "validate_scaling" {
  input = {
    min_instances    = var.min_instances
    max_instances    = var.max_instances
    redis_enabled    = var.redis_enabled
    vpc_connector_id = var.vpc_connector_id
  }

  lifecycle {
    precondition {
      condition     = var.max_instances >= var.min_instances
      error_message = "Configuration invalide: max_instances (${var.max_instances}) doit être >= min_instances (${var.min_instances})."
    }

    precondition {
      condition     = !var.redis_enabled || try(trimspace(var.vpc_connector_id) != "", false)
      error_message = "Configuration invalide: redis_enabled=true exige un vpc_connector_id non nul pour conserver l'accès Redis via le VPC connector."
    }
  }
}

# ------------------------------------------------------------------------------
# Locals : env vars finales
# - On injecte JAVA_TOOL_OPTIONS si inject_cloud_run_port = true
# - On filtre TOUJOURS "PORT" (réservé Cloud Run)
# ------------------------------------------------------------------------------
locals {
  # ✅ Pour Spring Boot : force server.port = var.port
  # NOTE: ne pas mettre PORT ici ! Cloud Run le gère.
  base_env_vars = var.inject_cloud_run_port ? {
    JAVA_TOOL_OPTIONS = "-Dserver.port=${var.port}"
  } : {}

  # Merge : tes env vars custom + base env vars
  final_env_vars = merge(local.base_env_vars, var.env_vars)
  has_vpc_connector = try(trimspace(var.vpc_connector_id) != "", false)
}

resource "google_cloud_run_v2_service" "this" {
  depends_on = [terraform_data.validate_scaling]

  lifecycle {
    # Infra-only Terraform strategy: app image rollouts are handled by gcloud run deploy.
    ignore_changes = [
      client,
      client_version,
      template[0].containers[0].image
    ]
  }

  name     = var.name
  location = var.region
  project  = var.project_id

  ingress = var.ingress
  labels  = coalesce(var.labels, {})

  template {
    service_account  = var.service_account_email

    scaling {
      min_instance_count = var.min_instances
      max_instance_count = var.max_instances
    }

    # --------------------------------------------------------------------------
    # (OPTIONNEL) VPC Access Connector
    # --------------------------------------------------------------------------
    dynamic "vpc_access" {
      for_each = local.has_vpc_connector ? [var.vpc_connector_id] : []
      content {
        connector = vpc_access.value
        egress    = var.vpc_egress
      }
    }

    # --------------------------------------------------------------------------
    # (OPTIONNEL) Volume Cloud SQL Connector
    # --------------------------------------------------------------------------
    dynamic "volumes" {
      for_each = length(var.cloudsql_instances) > 0 ? [1] : []
      content {
        name = "cloudsql"
        cloud_sql_instance {
          instances = var.cloudsql_instances
        }
      }
    }

    containers {
      image = var.image

      # Cloud Run route vers ce port côté container
      ports {
        container_port = var.port
      }

      # ✅ Startup probe : donne plus de temps aux services lents (DB + Config Server)
      startup_probe {
        tcp_socket {
          port = var.port
        }
        initial_delay_seconds = 10
        period_seconds        = 10
        failure_threshold     = 30
        timeout_seconds       = 5
      }

      resources {
        limits = {
          cpu    = var.cpu
          memory = var.memory
        }
        startup_cpu_boost = var.cpu_boost
        cpu_idle          = var.cpu_idle
      }

      # ------------------------------------------------------------------------
      # ✅ ENV VARS : ordre stable + filtre PORT (réservé Cloud Run)
      # ------------------------------------------------------------------------
      dynamic "env" {
        for_each = [
          for k in sort(keys(local.final_env_vars)) : {
            key   = k
            value = local.final_env_vars[k]
          }
          if k != "PORT"
        ]
        content {
          name  = env.value.key
          value = env.value.value
        }
      }

      # ------------------------------------------------------------------------
      # (OPTIONNEL) Mount Cloud SQL
      # ------------------------------------------------------------------------
      dynamic "volume_mounts" {
        for_each = length(var.cloudsql_instances) > 0 ? [1] : []
        content {
          name       = "cloudsql"
          mount_path = "/cloudsql"
        }
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }
}

resource "google_cloud_run_v2_service_iam_member" "public_invoker" {
  count    = var.allow_unauthenticated ? 1 : 0
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.this.name

  role   = "roles/run.invoker"
  member = "allUsers"
}
