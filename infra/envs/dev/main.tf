# ==============================================================================
# infra/envs/dev/main.tf
# ==============================================================================
# ENVIRONNEMENT : DEV (microservices complet avec Config Server)
#
# Déploie :
# - Artifact Registry
# - IAM (GitLab CI SA + Cloud Run runtime SA)
# - WIF GitLab OIDC
# - Cloud SQL (instance partagée + 3 DB)
# - Redis (Memorystore)
# - VPC Connector (Cloud Run -> Redis)
# - Pub/Sub (topics + subscriptions + DLQ + IAM)
# - Cloud Run : eureka-server, config-server, api-gateway, auth-service
# ==============================================================================

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.30"
    }
    time = {
      source  = "hashicorp/time"
      version = "~> 0.9.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

data "google_project" "current" {
  project_id = var.project_id
}

# ==============================================================================
# ACTIVATION DES APIs (EN PREMIER !)
# ==============================================================================
module "common" {
  source     = "../../modules/common"
  project_id = var.project_id
}

# ==============================================================================
# ARTIFACT REGISTRY
# ==============================================================================
module "artifact_registry" {
  source        = "../../modules/artifact_registry"
  project_id    = var.project_id
  region        = var.region
  repository_id = var.artifact_repo_id

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }

  depends_on = [module.common]
}

# ==============================================================================
# IAM
# ==============================================================================
module "iam" {
  source      = "../../modules/iam"
  project_id  = var.project_id
  environment = var.environment

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }

  depends_on = [module.common]
}

# ==============================================================================
# WIF GitLab (OIDC)
# ==============================================================================
module "wif_gitlab" {
  source      = "../../modules/wif_gitlab"
  project_id  = var.project_id
  environment = var.environment

  gitlab_project_path   = "Mohammed_KHNine/speedline"
  service_account_email = module.iam.gitlab_ci_sa_email
  ref_prefix            = "dev-"

  depends_on = [module.common, module.iam]
}

# ==============================================================================
# CLOUD SQL (instance partagée)
# ==============================================================================
module "cloudsql" {
  source     = "../../modules/cloudsql"
  project_id = var.project_id
  region     = var.region

  instance_name    = var.cloudsql_instance_name
  database_version = "POSTGRES_15"
  tier             = var.cloudsql_tier

  databases = [
    "speedline_dev",
    "speedline_staging",
    "speedline_prod"
  ]

  users = {
    dev = {
      name     = "auth_dev"
      password = var.db_dev_password
      database = "speedline_dev"
    }
    staging = {
      name     = "auth_staging"
      password = var.db_staging_password
      database = "speedline_staging"
    }
    prod = {
      name     = "auth_prod"
      password = var.db_prod_password
      database = "speedline_prod"
    }
  }

  enable_public_ip    = true
  authorized_networks = []
  backup_enabled      = false
  deletion_protection = false

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }

  depends_on = [module.common]
}

# ==============================================================================
# VPC CONNECTOR (Cloud Run -> VPC pour Redis)
# ==============================================================================
module "vpc_connector" {
  source     = "../../modules/vpc_connector"
  project_id = var.project_id
  region     = var.region

  name          = var.vpc_connector_name
  network       = "default"
  ip_cidr_range = var.vpc_connector_cidr

  depends_on = [module.common]
}

# ==============================================================================
# REDIS Memorystore
# ==============================================================================
module "redis" {
  source     = "../../modules/redis"
  project_id = var.project_id
  region     = var.region

  name           = var.redis_instance_name
  tier           = "BASIC"
  memory_size_gb = 1
  redis_version  = "REDIS_7_0"
  network        = "default"

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }

  depends_on = [module.common]
}

# ==============================================================================
# PUB/SUB (Topics + Subs + DLQ + IAM)
# ==============================================================================
module "pubsub" {
  source     = "../../modules/pubsub"
  project_id = var.project_id

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }

  topics = [
    "auth-events",
    "email-events"
  ]

  dlq_topic = "speedline-dlq"

  subscriptions = {
    "auth-events-sub" = {
      topic                 = "auth-events"
      ack_deadline_seconds  = 30
      max_delivery_attempts = 5
      minimum_backoff       = "10s"
      maximum_backoff       = "600s"
    }

    "email-events-sub" = {
      topic                 = "email-events"
      ack_deadline_seconds  = 30
      max_delivery_attempts = 5
      minimum_backoff       = "10s"
      maximum_backoff       = "600s"
    }
  }

  publisher_service_accounts = [
    module.iam.cloudrun_runtime_sa_email
  ]

  subscriber_service_accounts = [
    module.iam.cloudrun_runtime_sa_email
  ]

  depends_on = [module.common, module.iam]
}

# ==============================================================================
# MONGODB ATLAS (notification-service)
# ------------------------------------------------------------------------------
# M0/FREE est souvent géré manuellement (UI Atlas). Ce module conserve les
# paramètres/outputs pour une infra reproductible sans casser le workflow actuel.
# ==============================================================================
module "mongo_atlas" {
  source = "../../modules/mongo_atlas"

  manage_resources   = var.enable_mongo_atlas_terraform
  atlas_public_key   = var.atlas_public_key
  atlas_private_key  = var.atlas_private_key
  atlas_org_id       = var.atlas_org_id
  atlas_project_id   = var.atlas_project_id
  atlas_cluster_name = var.atlas_cluster_name
  atlas_region       = var.atlas_region

  mongo_db_name      = var.mongo_db_name
  mongo_db_username  = var.mongo_db_username
  mongo_db_password  = var.mongo_db_password
  mongo_allowed_cidr = var.mongo_allowed_cidr

  mongodb_uri = var.mongodb_uri
}

# ==============================================================================
# CLOUD RUN SERVICES
# ==============================================================================

# ------------------------------------------------------------------------------
# EUREKA SERVER
# ------------------------------------------------------------------------------
module "eureka_server" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "eureka-server"
  image = var.images["eureka-server"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = true # ✅ Force port 8080 via JAVA_TOOL_OPTIONS
  min_instances         = 0

  env_vars = {
    SPRING_PROFILES_ACTIVE  = "dev"
    SPRING_APPLICATION_NAME = "eureka-server"
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }
}

# ------------------------------------------------------------------------------
# CONFIG SERVER (IMPORTANT)
# ------------------------------------------------------------------------------
module "config_server" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "config-server"
  image = var.images["config-server"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = true # ✅ Force port 8080 via JAVA_TOOL_OPTIONS
  min_instances         = 0

  env_vars = {
    SPRING_PROFILES_ACTIVE  = "dev"
    SPRING_APPLICATION_NAME = "config-server"
    EUREKA_ENABLED          = "false"

    # ✅ credentials basiques en DEV (en prod => Secret Manager)
    CONFIG_USER     = "config"
    CONFIG_PASSWORD = "config123"

    # Spring Cloud Config Server Git backend
    SPRING_CLOUD_CONFIG_SERVER_GIT_URI           = var.config_server_git_uri
    SPRING_CLOUD_CONFIG_SERVER_GIT_DEFAULT_LABEL = var.config_server_git_default_label
    SPRING_CLOUD_CONFIG_SERVER_GIT_USERNAME      = var.config_server_git_username
    SPRING_CLOUD_CONFIG_SERVER_GIT_PASSWORD      = var.config_server_git_password
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }
}

# ------------------------------------------------------------------------------
# API GATEWAY
# ------------------------------------------------------------------------------
module "api_gateway" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "api-gateway"
  image = var.images["api-gateway"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = true # ✅ Force port 8080 via JAVA_TOOL_OPTIONS
  min_instances         = 1    # ✅ Éviter les cold starts
  cpu_boost             = true # ✅ Réduit le cold start au démarrage

  env_vars = {
    SPRING_PROFILES_ACTIVE = "dev"
    GCP_PROJECT_ID         = var.project_id
    EUREKA_ENABLED         = "false"
    # IMPORTANT: même secret que auth-service pour valider correctement les JWT signés.
    JWT_SECRET                      = var.jwt_secret
    SPRING_MAIN_LAZY_INITIALIZATION = "true"
    SPRING_CLOUD_DISCOVERY_ENABLED  = "false"
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }
}

# ------------------------------------------------------------------------------
# AUTH SERVICE (CloudSQL + Redis + Pub/Sub + Config Server)
# ------------------------------------------------------------------------------
module "auth_service" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "auth-service"
  image = var.images["auth-service"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = false # JAVA_TOOL_OPTIONS géré manuellement dans env_vars
  min_instances         = 0     # ✅ Scale to zero — warmed up via Cloud Scheduler
  cpu_boost             = true  # ✅ Réduit le cold start au démarrage

  # ✅ Augmenter la mémoire pour auth-service (DB + Redis + JWT)
  memory = "1Gi"
  cpu    = "1"

  cloudsql_instances = [module.cloudsql.instance_connection_name]

  vpc_connector_id = module.vpc_connector.id
  vpc_egress       = "PRIVATE_RANGES_ONLY"

  env_vars = {
    # Spring
    SPRING_PROFILES_ACTIVE  = "dev"
    SPRING_APPLICATION_NAME = "auth-service"
    GCP_PROJECT_ID          = var.project_id
    EUREKA_ENABLED          = "false"

    # ✅ JAVA_TOOL_OPTIONS: flags JVM appliqués AVANT le chargement des YAMLs Spring
    # -Dserver.port=8080                            : Cloud Run impose PORT=8080
    # -Dspring.cloud.bootstrap.enabled=false        : désactive Bootstrap Context
    # -Dspring.cloud.config.enabled=false           : désactive Config Client
    # -Dspring.cloud.gcp.sql.enabled=false          : désactive GCP SQL autoconfigure
    # -Dspring.cloud.gcp.core.enabled=false         : désactive GCP Core autoconfigure
    # Ces flags sont évalués par les EnvironmentPostProcessors AVANT les YAMLs.
    # Ils empêchent CloudSqlEnvironmentPostProcessor (spring-cloud-gcp-autoconfigure)
    # de s'exécuter et crasher avec "A database name must be provided".
    JAVA_TOOL_OPTIONS = "-Dserver.port=8080 -Dspring.cloud.bootstrap.enabled=false -Dspring.cloud.config.enabled=false -Dspring.cloud.gcp.sql.enabled=false -Dspring.cloud.gcp.core.enabled=false"

    # Config Server désactivé (config via env vars)
    SPRING_CLOUD_CONFIG_ENABLED = "false"
    CONFIG_SERVER_URL           = module.config_server.service_url
    CONFIG_USER                 = "config"
    CONFIG_PASSWORD             = "config123"

    # DB - Cloud SQL via postgres-socket-factory (connecteur JDBC officiel GCP)
    # Format: jdbc:postgresql:///DB_NAME?cloudSqlInstance=PROJECT:REGION:INSTANCE&socketFactory=...
    # socketFactory=com.google.cloud.sql.postgres.SocketFactory active le connecteur Cloud SQL.
    # NB: spring-cloud-gcp-autoconfigure est EXCLU du JAR (scope provided) pour éviter le crash
    # "A database name must be provided" au démarrage. La connection passe par socketFactory pur.
    SPRING_DATASOURCE_URL      = "jdbc:postgresql:///speedline_dev?cloudSqlInstance=${module.cloudsql.instance_connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
    SPRING_DATASOURCE_USERNAME = "auth_dev"
    SPRING_DATASOURCE_PASSWORD = var.db_dev_password

    # Redis
    SPRING_DATA_REDIS_HOST = module.redis.host
    SPRING_DATA_REDIS_PORT = tostring(module.redis.port)
    CACHE_KEY_PREFIX       = "dev:"

    # Pub/Sub
    PUBSUB_AUTH_TOPIC  = "auth-events"
    PUBSUB_EMAIL_TOPIC = "email-events"
    PUBSUB_AUTH_SUB    = "auth-events-sub"
    PUBSUB_EMAIL_SUB   = "email-events-sub"
    PUBSUB_DLQ_TOPIC   = module.pubsub.dlq_topic

    # ✅ JWT obligatoire sinon ton app fail au boot
    JWT_SECRET = var.jwt_secret

    # Feign inter-service URLs (Eureka désactivé en DEV — communication directe Cloud Run)
    PARTNER_SERVICE_URL = "https://partner-service-392205979525.europe-west1.run.app"
    USER_SERVICE_URL    = "https://user-service-392205979525.europe-west1.run.app"

    SPRING_MAIN_LAZY_INITIALIZATION = "true"
    SPRING_CLOUD_DISCOVERY_ENABLED  = "false"
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }
}

# ------------------------------------------------------------------------------
# USER SERVICE (CloudSQL)
# ------------------------------------------------------------------------------
module "user_service" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "user-service"
  image = var.images["user-service"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = false
  min_instances         = 0    # ✅ Scale to zero
  cpu_boost             = true # ✅ Réduit le cold start au démarrage

  memory = "1Gi"
  cpu    = "1"

  cloudsql_instances = [module.cloudsql.instance_connection_name]

  env_vars = {
    SPRING_PROFILES_ACTIVE  = "dev"
    SPRING_APPLICATION_NAME = "user-service"
    EUREKA_ENABLED          = "false"
    JAVA_TOOL_OPTIONS       = "-Dserver.port=8080 -Dspring.cloud.bootstrap.enabled=false -Dspring.cloud.config.enabled=false -Dspring.cloud.gcp.sql.enabled=false -Dspring.cloud.gcp.core.enabled=false"

    SPRING_CLOUD_GCP_SQL_ENABLED  = "false"
    SPRING_CLOUD_GCP_CORE_ENABLED = "false"

    # Cloud SQL socket factory (même approche que auth-service)
    SPRING_DATASOURCE_URL      = "jdbc:postgresql:///speedline_dev?cloudSqlInstance=${module.cloudsql.instance_connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
    SPRING_DATASOURCE_USERNAME = "auth_dev"
    SPRING_DATASOURCE_PASSWORD = var.db_dev_password

    SPRING_MAIN_LAZY_INITIALIZATION = "true"
    SPRING_CLOUD_DISCOVERY_ENABLED  = "false"
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }
}

# ------------------------------------------------------------------------------
# PARTNER SERVICE (CloudSQL + Pub/Sub)
# ------------------------------------------------------------------------------
module "partner_service" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "partner-service"
  image = var.images["partner-service"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = false
  min_instances         = 0    # ✅ Scale to zero
  cpu_boost             = true # ✅ Réduit le cold start au démarrage

  memory = "1Gi"
  cpu    = "1"

  cloudsql_instances = [module.cloudsql.instance_connection_name]

  env_vars = {
    SPRING_PROFILES_ACTIVE       = "dev"
    SPRING_APPLICATION_NAME      = "partner-service"
    EUREKA_ENABLED               = "false"
    JAVA_TOOL_OPTIONS            = "-Dserver.port=8080 -Dspring.cloud.bootstrap.enabled=false -Dspring.cloud.config.enabled=false -Dspring.cloud.gcp.sql.enabled=false"
    SPRING_AUTOCONFIGURE_EXCLUDE = "com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubEmulatorAutoConfiguration"

    SPRING_CLOUD_GCP_SQL_ENABLED  = "false"
    SPRING_CLOUD_GCP_CORE_ENABLED = "true"
    SPRING_CLOUD_GCP_PROJECT_ID   = var.project_id

    SPRING_DATASOURCE_URL      = "jdbc:postgresql:///speedline_dev?cloudSqlInstance=${module.cloudsql.instance_connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
    SPRING_DATASOURCE_USERNAME = "auth_dev"
    SPRING_DATASOURCE_PASSWORD = var.db_dev_password
    UPLOAD_DIR                 = "/tmp/uploads"

    # GCP Pub/Sub runtime
    GCP_PROJECT_ID = var.project_id

    # Feign inter-service URLs (Eureka désactivé en DEV — communication directe Cloud Run)
    AUTH_SERVICE_URL = "https://auth-service-392205979525.europe-west1.run.app"

    SPRING_MAIN_LAZY_INITIALIZATION = "true"
    SPRING_CLOUD_DISCOVERY_ENABLED  = "false"
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }
}

# ------------------------------------------------------------------------------
# LOCATION SERVICE (CloudSQL)
# ------------------------------------------------------------------------------
module "location_service" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "location-service"
  image = var.images["location-service"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = false
  min_instances         = 0
  cpu_boost             = true # ✅ Réduit le cold start au démarrage

  memory = "1Gi"
  cpu    = "1"

  cloudsql_instances = [module.cloudsql.instance_connection_name]

  env_vars = {
    SPRING_PROFILES_ACTIVE  = "dev"
    SPRING_APPLICATION_NAME = "location-service"
    EUREKA_ENABLED          = "false"
    JAVA_TOOL_OPTIONS       = "-Dserver.port=8080 -Dspring.cloud.bootstrap.enabled=false -Dspring.cloud.config.enabled=false -Dspring.cloud.gcp.sql.enabled=false -Dspring.cloud.gcp.core.enabled=false"

    SPRING_CLOUD_GCP_SQL_ENABLED  = "false"
    SPRING_CLOUD_GCP_CORE_ENABLED = "false"

    SPRING_DATASOURCE_URL      = "jdbc:postgresql:///speedline_dev?cloudSqlInstance=${module.cloudsql.instance_connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
    SPRING_DATASOURCE_USERNAME = "auth_dev"
    SPRING_DATASOURCE_PASSWORD = var.db_dev_password

    SPRING_MAIN_LAZY_INITIALIZATION = "true"
    SPRING_CLOUD_DISCOVERY_ENABLED  = "false"
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }
}

# ------------------------------------------------------------------------------
# NOTIFICATION SERVICE (MongoDB Atlas + Pub/Sub)
# ------------------------------------------------------------------------------
module "notification_service" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "notification-service"
  image = var.images["notification-service"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = true
  min_instances         = 1 # ✅ Pub/Sub PULL nécessite une instance active en continu
  max_instances         = 1 # ✅ Limite coût/empreinte sur le service de notification en DEV
  cpu_boost             = true
  cpu_idle              = false # ✅ CPU always allocated pour thread subscriber Pub/Sub en arrière-plan

  memory = "512Mi"
  cpu    = "1"

  env_vars = {
    SPRING_PROFILES_ACTIVE  = "dev"
    SPRING_APPLICATION_NAME = "notification-service"
    EUREKA_ENABLED          = "false"
    GCP_PROJECT_ID          = var.project_id

    # MongoDB Atlas URI injectée depuis GitLab CI/CD (TF_VAR_mongodb_uri)
    MONGODB_URI = var.mongodb_uri

    JAVA_TOOL_OPTIONS = "-Dspring.cloud.bootstrap.enabled=false -Dspring.cloud.config.enabled=false"

    SPRING_MAIN_LAZY_INITIALIZATION = "true"
    SPRING_CLOUD_DISCOVERY_ENABLED  = "false"
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
  }
}

# ------------------------------------------------------------------------------
# PARTNER DASHBOARD (Angular)
# ------------------------------------------------------------------------------
module "partner_dashboard" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "partner-dashboard"
  image = var.images["partner-dashboard"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = false
  min_instances         = 0

  env_vars = {
    ENV          = var.frontend_env
    API_BASE_URL = var.frontend_api_base_url
    WS_URL       = var.frontend_ws_url
    APP_NAME     = "SpeedLine Partner Dashboard"
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
    tier       = "frontend"
  }
}

# ------------------------------------------------------------------------------
# ADMIN PANEL (Angular)
# ------------------------------------------------------------------------------
module "admin_panel" {
  source     = "../../modules/cloudrun_service"
  project_id = var.project_id
  region     = var.region

  name  = "admin-panel"
  image = var.images["admin-panel"]

  service_account_email = module.iam.cloudrun_runtime_sa_email
  allow_unauthenticated = true
  inject_cloud_run_port = false
  min_instances         = 0

  env_vars = {
    ENV                   = var.frontend_env
    API_BASE_URL          = var.frontend_api_base_url
    API_URL               = "${var.frontend_api_base_url}/api/v1"
    UPLOADS_BASE_URL      = var.frontend_api_base_url
    NOTIFICATIONS_API_URL = "${var.frontend_api_base_url}/api"
    WS_URL                = var.frontend_ws_url
    APP_NAME              = "SpeedLine Admin Panel"
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
    tier       = "frontend"
  }
}

# ==============================================================================
# CLOUD SCHEDULER — API GATEWAY WARM-UP (DEV)
# Maintient api-gateway chaud pour éviter les cold starts utilisateur.
# Appel GET /actuator/health toutes les 5 minutes.
# Pas d'auth OIDC nécessaire : api-gateway accepte le trafic non authentifié.
# ==============================================================================
resource "google_cloud_scheduler_job" "api_gateway_warmup_dev" {
  depends_on = [module.common, module.api_gateway]

  name             = "api-gateway-warmup-dev"
  description      = "Periodic GET /actuator/health on api-gateway to prevent cold starts (DEV)"
  schedule         = "*/5 * * * *"
  time_zone        = "UTC"
  attempt_deadline = "30s"
  region           = var.region
  project          = var.project_id

  http_target {
    uri         = "${module.api_gateway.uri}/actuator/health"
    http_method = "GET"
  }
}

# ==============================================================================
# CLOUD SCHEDULER — NOTIFICATION SERVICE WARM-UP (DEV)
# Réveille périodiquement notification-service pour permettre au subscriber
# Pub/Sub pull de traiter les messages même avec min_instances = 0.
# ==============================================================================
resource "google_cloud_scheduler_job" "notification_service_warmup_dev" {
  depends_on = [module.common, module.notification_service]

  name             = "notification-service-warmup-dev"
  description      = "Periodic GET /actuator/health on notification-service to wake up pull subscriber (DEV)"
  schedule         = var.notification_warmup_schedule
  time_zone        = "UTC"
  attempt_deadline = "30s"
  region           = var.region
  project          = var.project_id

  http_target {
    uri         = "${module.notification_service.uri}/actuator/health"
    http_method = "GET"
  }
}

# ==============================================================================
# PUB/SUB PUSH — IAM (DEV)
# Allows Pub/Sub service agent to invoke notification-service Cloud Run
# and to generate OIDC tokens using the runtime service account.
# These are required for authenticated push subscriptions.
# ==============================================================================
resource "google_cloud_run_v2_service_iam_member" "notification_service_pubsub_invoker_dev" {
  depends_on = [module.common, module.notification_service]

  project  = var.project_id
  location = var.region
  name     = module.notification_service.name

  role   = "roles/run.invoker"
  member = "serviceAccount:service-${data.google_project.current.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

resource "google_service_account_iam_member" "notification_push_oidc_token_creator_dev" {
  depends_on = [module.common, module.iam]

  service_account_id = "projects/${var.project_id}/serviceAccounts/${module.iam.cloudrun_runtime_sa_email}"
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = "serviceAccount:service-${data.google_project.current.number}@gcp-sa-pubsub.iam.gserviceaccount.com"
}

# ==============================================================================
# PUB/SUB PUSH PREP (DEV) - PRÉPARATION DE MIGRATION SANS CHANGER LE CODE APP
# ------------------------------------------------------------------------------
# Cette section prépare l'infra pour un futur mode Push:
# - autorise Pub/Sub service agent à invoquer notification-service
# - autorise Pub/Sub service agent à signer un OIDC token via runtime SA
# - crée OPTIONNELLEMENT une subscription push (désactivée par défaut)
#
# ⚠️ Tant qu'aucun endpoint applicatif compatible Pub/Sub push n'existe,
# garder notification_push_subscription_enabled = false.
# ==============================================================================
resource "google_pubsub_subscription" "notification_push_prep_dev" {
  count = var.notification_push_subscription_enabled ? 1 : 0

  depends_on = [
    module.common,
    module.pubsub,
    google_cloud_run_v2_service_iam_member.notification_service_pubsub_invoker_dev,
    google_service_account_iam_member.notification_push_oidc_token_creator_dev
  ]

  project = var.project_id
  name    = var.notification_push_subscription_name
  topic   = "projects/${var.project_id}/topics/${var.notification_push_source_topic}"

  ack_deadline_seconds       = 30
  message_retention_duration = "604800s"
  retain_acked_messages      = false

  retry_policy {
    minimum_backoff = "10s"
    maximum_backoff = "600s"
  }

  push_config {
    push_endpoint = "${module.notification_service.uri}${var.notification_push_endpoint_path}"
    oidc_token {
      service_account_email = module.iam.cloudrun_runtime_sa_email
      audience              = module.notification_service.uri
    }
  }

  labels = {
    env        = var.environment
    managed-by = "terraform"
    project    = "speedline"
    purpose    = "push-prep"
  }
}
