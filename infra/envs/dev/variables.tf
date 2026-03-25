# ------------------------------------------------------------
# Variables d'entrée de l'environnement DEV
# ------------------------------------------------------------

variable "project_id" {
  description = "ID du projet Google Cloud (DEV)"
  type        = string
}

variable "region" {
  description = "Région principale GCP"
  type        = string
}

variable "environment" {
  description = "Nom de l'environnement"
  type        = string
}

variable "artifact_repo_id" {
  description = "Nom du repository Artifact Registry"
  type        = string
  default     = "speedline-docker"
}

variable "images" {
  description = "Docker images par service (ex: api-gateway => full image uri)"
  type        = map(string)
  default     = {}
}

variable "labels" {
  description = "Labels communs pour les ressources"
  type        = map(string)
  default     = {}
}

variable "enable_project_iam_bindings" {
  description = "Active les bindings IAM de niveau projet dans le module IAM. Laisser false en CI pour éviter les erreurs 403 setIamPolicy."
  type        = bool
  default     = false
}

# ------------------------------------------------------------------------------
# Cloud SQL (instance partagée + 3 bases)
# ------------------------------------------------------------------------------
variable "cloudsql_instance_name" {
  description = "Nom de l'instance Cloud SQL partagée (host = DEV)"
  type        = string
  default     = "speedline-shared-sql"
}

variable "cloudsql_tier" {
  description = "Taille instance Cloud SQL"
  type        = string
  default     = "db-custom-1-3840"
}

# Passwords (sensibles)
variable "db_dev_password" {
  description = "Mot de passe user DB dev"
  type        = string
  sensitive   = true
}

variable "db_staging_password" {
  description = "Mot de passe user DB staging"
  type        = string
  sensitive   = true
}

variable "db_prod_password" {
  description = "Mot de passe user DB prod"
  type        = string
  sensitive   = true
}

# ------------------------------------------------------------------------------
# Cross-project : runtime SA emails staging/prod (pour donner cloudsql.client)
# ------------------------------------------------------------------------------
variable "staging_runtime_sa_email" {
  description = "Email du service account runtime Cloud Run du projet STAGING"
  type        = string
  default     = ""
}

variable "prod_runtime_sa_email" {
  description = "Email du service account runtime Cloud Run du projet PROD"
  type        = string
  default     = ""
}



# ------------------------------------------------------------------------------
# Redis (Memorystore)
# ------------------------------------------------------------------------------
variable "redis_instance_name" {
  description = "Nom de l'instance Redis (partagée)"
  type        = string
  default     = "speedline-redis-shared"
}

# ------------------------------------------------------------------------------
# VPC Connector (Cloud Run -> VPC pour accéder à Redis)
# ------------------------------------------------------------------------------
variable "vpc_connector_name" {
  description = "Nom du Serverless VPC Access Connector"
  type        = string
  default     = "serverless-conn-dev"
}

variable "vpc_connector_cidr" {
  description = "CIDR /28 obligatoire pour le connector (ex: 10.8.0.0/28)"
  type        = string
  default     = "10.8.0.0/28"
}


# ------------------------------------------------------------------------------
# JWT (OBLIGATOIRE POUR AUTH-SERVICE)
# ------------------------------------------------------------------------------
variable "jwt_secret" {
  description = "Secret JWT utilisé par auth-service"
  type        = string
  sensitive   = true
}

# ------------------------------------------------------------------------------
# Config Server (Git backend)
# ------------------------------------------------------------------------------
variable "config_server_git_uri" {
  description = "Git repository URI used by Spring Cloud Config Server"
  type        = string
}

variable "config_server_git_default_label" {
  description = "Default Git branch/label used by Config Server"
  type        = string
  default     = "main"
}

variable "config_server_git_username" {
  description = "Git username/token user for Config Server repository access"
  type        = string
  sensitive   = true
  default     = ""
}

variable "config_server_git_password" {
  description = "Git password/token for Config Server repository access"
  type        = string
  sensitive   = true
  default     = ""
}

# ------------------------------------------------------------------------------
# Frontend Cloud Run
# ------------------------------------------------------------------------------
variable "frontend_env" {
  description = "Environment value injected into frontend containers"
  type        = string
  default     = "dev"
}

variable "frontend_api_base_url" {
  description = "Public API Gateway base URL consumed by frontend applications"
  type        = string
  default     = "https://api-gateway-392205979525.europe-west1.run.app"
}

variable "frontend_ws_url" {
  description = "Public websocket base URL consumed by frontend applications"
  type        = string
  default     = "wss://api-gateway-392205979525.europe-west1.run.app"
}

# ------------------------------------------------------------------------------
# Notification Service / MongoDB Atlas
# ------------------------------------------------------------------------------
variable "mongodb_uri" {
  description = "MongoDB Atlas connection URI for notification-service"
  type        = string
  sensitive   = true
}

variable "mongo_db_name" {
  description = "MongoDB database name for notification-service"
  type        = string
  default     = "speedline_notification"
}

variable "enable_mongo_atlas_terraform" {
  description = "Enable Terraform-managed MongoDB Atlas resources (false keeps Atlas managed manually)"
  type        = bool
  default     = false
}

variable "atlas_public_key" {
  description = "MongoDB Atlas API public key"
  type        = string
  sensitive   = true
  default     = ""
}

variable "atlas_private_key" {
  description = "MongoDB Atlas API private key"
  type        = string
  sensitive   = true
  default     = ""
}

variable "atlas_org_id" {
  description = "MongoDB Atlas organization ID"
  type        = string
  default     = ""
}

variable "atlas_project_id" {
  description = "MongoDB Atlas project ID"
  type        = string
  default     = ""
}

variable "atlas_cluster_name" {
  description = "MongoDB Atlas cluster name"
  type        = string
  default     = "Cluster0"
}

variable "atlas_region" {
  description = "MongoDB Atlas region"
  type        = string
  default     = "EU_WEST_1"
}

variable "mongo_db_username" {
  description = "MongoDB Atlas database username"
  type        = string
  default     = ""
}

variable "mongo_db_password" {
  description = "MongoDB Atlas database password"
  type        = string
  sensitive   = true
  default     = ""
}

variable "mongo_allowed_cidr" {
  description = "MongoDB Atlas access list CIDR"
  type        = string
  default     = "0.0.0.0/0"
}

# ------------------------------------------------------------------------------
# Notification service - DEV cost/runtime tuning & push migration prep
# ------------------------------------------------------------------------------
variable "notification_warmup_schedule" {
  description = "Cron Cloud Scheduler pour réveiller notification-service en DEV"
  type        = string
  default     = "*/10 * * * *"
}

variable "notification_push_subscription_enabled" {
  description = "Active la subscription Pub/Sub push de préparation (DEV). Laisser false tant qu'aucun endpoint push compatible n'existe côté app."
  type        = bool
  default     = false
}

variable "notification_push_subscription_name" {
  description = "Nom de la subscription push de préparation (DEV)"
  type        = string
  default     = "notification-push-dev"
}

variable "notification_push_source_topic" {
  description = "Topic source pour la subscription push de préparation (DEV)"
  type        = string
  default     = "email-events"
}

variable "notification_push_endpoint_path" {
  description = "Path HTTP du endpoint Cloud Run qui recevra les push Pub/Sub (doit exister côté app avant activation)."
  type        = string
  default     = "/pubsub/push"
}