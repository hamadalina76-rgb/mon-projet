# ==============================================================================
# modules/cloudrun_service/variables.tf
# ==============================================================================

variable "project_id" {
  description = "ID du projet GCP"
  type        = string
}

variable "region" {
  description = "Région Cloud Run"
  type        = string
}

variable "name" {
  description = "Nom du service Cloud Run"
  type        = string
}

variable "image" {
  description = "Image Docker complète"
  type        = string
}

variable "service_account_email" {
  description = "Service Account runtime utilisé par Cloud Run"
  type        = string
}

variable "port" {
  description = "Port exposé par le container (Cloud Run attend que l'app écoute dessus)"
  type        = number
  default     = 8080

  validation {
    condition     = var.port > 0 && var.port < 65536
    error_message = "Le port doit être entre 1 et 65535."
  }
}

# ✅ Nouveau : injecter automatiquement PORT + JAVA_TOOL_OPTIONS
variable "inject_cloud_run_port" {
  description = "Injecte PORT et force Spring Boot à écouter sur var.port via JAVA_TOOL_OPTIONS"
  type        = bool
  default     = true
}

variable "env_vars" {
  description = "Variables d'environnement"
  type        = map(string)
  default     = {}
}

variable "allow_unauthenticated" {
  description = "Autoriser accès public"
  type        = bool
  default     = false
}

variable "labels" {
  description = "Labels GCP"
  type        = map(string)
  default     = {}
}

variable "cpu" {
  description = "CPU Cloud Run"
  type        = string
  default     = "1"

  validation {
    condition     = contains(["1", "2", "4"], var.cpu)
    error_message = "CPU autorisé : 1, 2 ou 4."
  }
}

variable "memory" {
  description = "RAM Cloud Run"
  type        = string
  default     = "512Mi"

  validation {
    condition     = can(regex("^[0-9]+(Mi|Gi)$", var.memory))
    error_message = "Format mémoire invalide."
  }
}

variable "min_instances" {
  description = "Nombre minimum d’instances"
  type        = number
  default     = 0

  validation {
    condition     = var.min_instances >= 0
    error_message = "min_instances doit être >= 0."
  }
}

variable "max_instances" {
  description = "Nombre maximum d’instances"
  type        = number
  default     = 3

  validation {
    condition     = var.max_instances >= 0
    error_message = "max_instances doit être >= 0."
  }
}

variable "ingress" {
  description = "Type d'ingress Cloud Run"
  type        = string
  default     = "INGRESS_TRAFFIC_ALL"

  validation {
    condition = contains([
      "INGRESS_TRAFFIC_ALL",
      "INGRESS_TRAFFIC_INTERNAL_ONLY",
      "INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER"
    ], var.ingress)
    error_message = "Valeur ingress invalide."
  }
}

variable "cloudsql_instances" {
  description = "Instances Cloud SQL attachées"
  type        = list(string)
  default     = []
}

variable "vpc_connector_id" {
  description = "ID du VPC Connector"
  type        = string
  default     = ""
}

variable "vpc_egress" {
  description = "Sortie réseau via VPC connector"
  type        = string
  default     = "PRIVATE_RANGES_ONLY"

  validation {
    condition     = contains(["PRIVATE_RANGES_ONLY", "ALL_TRAFFIC"], var.vpc_egress)
    error_message = "vpc_egress invalide."
  }
}

# ✅ Startup probe timeout étendu pour services lents (DB + Config Server)
variable "startup_probe_timeout" {
  description = "Timeout en secondes pour le startup probe Cloud Run"
  type        = number
  default     = 240
}

# ✅ Startup CPU boost: alloue davantage de CPU au démarrage pour réduire le cold start
variable "cpu_boost" {
  description = "Active le CPU boost au démarrage du container (réduit le cold start)"
  type        = bool
  default     = false
}

# ✅ CPU always allocated : désactive le throttling CPU quand idle
# Nécessaire pour les services avec des background threads (ex: Pub/Sub streaming pull)
# false = CPU toujours alloué (recommandé pour Pub/Sub pull + min_instances >= 1)
# true  = CPU throttlé hors requêtes (défaut Cloud Run - économise des ressources)
variable "cpu_idle" {
  description = "Si true, le CPU est throttlé quand le container ne traite pas de requêtes. Mettre false pour les services avec threads background (Pub/Sub streaming pull)."
  type        = bool
  default     = true
}
