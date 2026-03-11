# ==============================================================================
# infra/modules/cloudsql/main.tf
# ==============================================================================
# Module Cloud SQL PostgreSQL
# - Crée 1 instance Cloud SQL
# - Crée N bases dans la même instance (ex: db-dev, db-staging, db-prod)
# - Crée des users dédiés
#
# NOTE (Solution B: 1 instance / 3 DB):
# - Tu peux héberger l'instance dans UN projet "host" (ex: dev)
# - Les Cloud Run des autres projets se connectent via Cloud SQL Connector
#   en utilisant instance_connection_name.
# ==============================================================================

resource "google_sql_database_instance" "this" {
  project          = var.project_id
  name             = var.instance_name
  region           = var.region
  database_version = var.database_version

  deletion_protection = var.deletion_protection

  settings {
    tier = var.tier

    # Disque
    disk_size       = var.disk_size_gb
    disk_type       = var.disk_type

    # Dev/Staging: ZONAL moins cher. Prod: REGIONAL si tu veux HA.
    availability_type = var.availability_type

    # Backups
    backup_configuration {
      enabled = var.backup_enabled
    }

    # Réseau
    # - Pour cross-project simple: ipv4 public = true + Cloud SQL Connector
    # - authorized_networks peut rester vide si tu n'autorises pas d'accès direct
    ip_configuration {
      ipv4_enabled = var.enable_public_ip

      dynamic "authorized_networks" {
        for_each = var.authorized_networks
        content {
          name  = authorized_networks.value.name
          value = authorized_networks.value.value
        }
      }
    }

    user_labels = var.labels
  }
}

# ------------------------------------------------------------------------------
# Création des bases
# ------------------------------------------------------------------------------
resource "google_sql_database" "db" {
  for_each = toset(var.databases)

  project  = var.project_id
  name     = each.value
  instance = google_sql_database_instance.this.name
}

# ------------------------------------------------------------------------------
# Création des users
# NB: un user Cloud SQL n'est pas “attaché” à une DB dans Terraform.
# On garde "database" juste pour ton organisation.
# ------------------------------------------------------------------------------
resource "google_sql_user" "users" {
  for_each = var.users

  project  = var.project_id
  instance = google_sql_database_instance.this.name

  name     = each.value.name
  password = each.value.password
}