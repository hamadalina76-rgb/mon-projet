# -------------------------------------------------------------------
# IAM MODULE : Service Accounts + rôles (propre)
# -------------------------------------------------------------------
# - gitlab-ci-sa-<env>           : utilisé par GitLab CI (build/push + deploy Cloud Run)
# - cloudrun-runtime-sa-<env>    : utilisé par Cloud Run au runtime (pull images + accès APIs)
#
# IMPORTANT :
# - La partie WIF (Workload Identity Federation) + impersonation du SA GitLab CI
#   (roles/iam.workloadIdentityUser + roles/iam.serviceAccountTokenCreator SUR LE SA)
#   est gérée dans le module: modules/wif_gitlab
# -------------------------------------------------------------------

# -------------------------------
# Service Accounts
# -------------------------------

# Service Account utilisé par GitLab CI
resource "google_service_account" "gitlab_ci" {
  project      = var.project_id
  account_id   = "gitlab-ci-sa-${var.environment}"
  display_name = "GitLab CI Service Account (${var.environment})"
}

# Service Account utilisé par Cloud Run au runtime
resource "google_service_account" "cloudrun_runtime" {
  project      = var.project_id
  account_id   = "cloudrun-runtime-sa-${var.environment}"
  display_name = "Cloud Run Runtime Service Account (${var.environment})"
}

# -------------------------------
# Rôles pour GitLab CI (CI/CD)
# -------------------------------

# Autorise GitLab CI à pousser les images dans Artifact Registry
resource "google_project_iam_member" "gitlab_artifact_writer" {
  count   = var.enable_project_iam_bindings ? 1 : 0
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.gitlab_ci.email}"
}

# Autorise GitLab CI à déployer/mettre à jour les services Cloud Run
resource "google_project_iam_member" "gitlab_run_admin" {
  count   = var.enable_project_iam_bindings ? 1 : 0
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.gitlab_ci.email}"
}

# Autorise GitLab CI à "utiliser" le runtime SA lors du déploiement
# (nécessaire pour --service-account lors du déploiement Cloud Run)
resource "google_project_iam_member" "gitlab_sa_user" {
  count   = var.enable_project_iam_bindings ? 1 : 0
  project = var.project_id
  role    = "roles/iam.serviceAccountUser"
  member  = "serviceAccount:${google_service_account.gitlab_ci.email}"
}

# NOTE : le CI SA a roles/editor attribué manuellement (hors Terraform)
# ce qui lui donne déjà les droits suffisants pour gérer IAM

# -------------------------------
# Rôles pour Cloud Run runtime (exécution)
# -------------------------------

# Permet au runtime Cloud Run de pull l'image depuis Artifact Registry
resource "google_project_iam_member" "runtime_artifact_reader" {
  count   = var.enable_project_iam_bindings ? 1 : 0
  project = var.project_id
  role    = "roles/artifactregistry.reader"
  member  = "serviceAccount:${google_service_account.cloudrun_runtime.email}"
}

# -------------------------------------------------------------------
# ⚠️  IMPORTANT : Les rôles suivants sont gérés MANUELLEMENT via gcloud
# car le CI SA n'a pas resourcemanager.projects.setIamPolicy
#
# À exécuter UNE SEULE FOIS par un Owner du projet :
#
#   gcloud projects add-iam-policy-binding PROJECT_ID \
#     --member="serviceAccount:cloudrun-runtime-sa-dev@PROJECT_ID.iam.gserviceaccount.com" \
#     --role="roles/cloudsql.client"
#
#   gcloud projects add-iam-policy-binding PROJECT_ID \
#     --member="serviceAccount:cloudrun-runtime-sa-dev@PROJECT_ID.iam.gserviceaccount.com" \
#     --role="roles/pubsub.editor"
#
#   gcloud projects add-iam-policy-binding PROJECT_ID \
#     --member="serviceAccount:cloudrun-runtime-sa-dev@PROJECT_ID.iam.gserviceaccount.com" \
#     --role="roles/serviceusage.serviceUsageConsumer"
#
# STATUS : ✅ Appliqués manuellement le 2026-03-03
# -------------------------------------------------------------------
