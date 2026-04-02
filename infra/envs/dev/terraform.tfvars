# ==============================================================================
# ENVIRONNEMENT : DEV
# Fichier non sensible (aucun secret ici)
# Les secrets sont injectés via GitLab CI variables (TF_VAR_*)
# ==============================================================================

# ------------------------------------------------------------------------------
# Projet & région
# ------------------------------------------------------------------------------
project_id  = "speedline-dev-488413"
region      = "europe-west1"
environment = "dev"

# ------------------------------------------------------------------------------
# IAM projet (désactivé pour CI standard)
# ------------------------------------------------------------------------------
enable_project_iam_bindings = false

# ------------------------------------------------------------------------------
# Artifact Registry
# ------------------------------------------------------------------------------
artifact_repo_id = "speedline-docker"

# ------------------------------------------------------------------------------
# Cloud SQL (instance partagée)
# ------------------------------------------------------------------------------
cloudsql_instance_name = "speedline-shared-sql"
cloudsql_tier          = "db-custom-1-3840"

# ------------------------------------------------------------------------------
# VPC Connector (Cloud Run → Redis)
# Doit matcher ton module vpc_connector
# ------------------------------------------------------------------------------
vpc_connector_name = "serverless-conn-dev"
vpc_connector_cidr = "10.8.0.0/28"

# ------------------------------------------------------------------------------
# Redis Memorystore
# ------------------------------------------------------------------------------
redis_instance_name = "speedline-redis-shared"

# ------------------------------------------------------------------------------
# Data services VM (préparation migration Redis/ClickHouse) - désactivé par défaut
# ------------------------------------------------------------------------------
enable_data_services_vm                  = false
data_services_vm_name                    = "data-services-dev"
data_services_vm_zone                    = "europe-west1-b"
data_services_vm_machine_type            = "e2-medium"
data_services_vm_disk_size_gb            = 40
data_services_vm_disk_type               = "pd-balanced"
data_services_vm_network                 = "default"
data_services_vm_subnetwork              = ""
data_services_vm_boot_image              = "debian-cloud/debian-12"
data_services_vm_service_account_email   = ""
data_services_vm_allowed_source_ranges   = []
data_services_vm_enable_iap_ssh          = true
data_services_vm_admin_ssh_source_ranges = []

# ------------------------------------------------------------------------------
# Notification service runtime cost tuning (DEV)
# ------------------------------------------------------------------------------
notification_warmup_schedule = "*/10 * * * *"

# ------------------------------------------------------------------------------
# Images (sera écrasé automatiquement par images.auto.tfvars.json en CI)
# Ne pas modifier manuellement en pipeline
# ------------------------------------------------------------------------------
images = {
  api-gateway          = "placeholder"
  config-server        = "placeholder"
  eureka-server        = "placeholder"
  auth-service         = "placeholder"
  user-service         = "placeholder"
  partner-service      = "placeholder"
  location-service     = "placeholder"
  delivery-service     = "placeholder"
  notification-service = "placeholder"
}
