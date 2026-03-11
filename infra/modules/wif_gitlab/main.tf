# -------------------------------------------------------------------
# Workload Identity Federation (GitLab.com) - SANS clés JSON
# -------------------------------------------------------------------
# Objectif :
# - GitLab CI s'authentifie via OIDC (token GitLab) -> GCP
# - Impersonation d'un Service Account sans JSON key
#
# Reco pro :
# - utiliser iam_member (pas iam_binding) => évite d'écraser d'autres bindings
# - restreindre sur project_path + ref_prefix AU NIVEAU GCP (en plus du CI)
#
# ✅ Fix important :
# - En CI, ton SA n'a pas les droits "iam.workloadIdentityPoolProviders.update"
# - Terraform essaye parfois de modifier attribute_condition => 403
# => On ajoute ignore_changes pour éviter les updates inutiles
# -------------------------------------------------------------------

locals {
  pool_id     = "gitlab-pool-${var.environment}"
  provider_id = "gitlab-provider-${var.environment}"

  service_account_full_id = "projects/${var.project_id}/serviceAccounts/${var.service_account_email}"

  principal_project = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.pool.name}/attribute.project_path/${var.gitlab_project_path}"
}

# -------------------------------------------------------------------
# 1) Workload Identity Pool
# -------------------------------------------------------------------
resource "google_iam_workload_identity_pool" "pool" {
  project                   = var.project_id
  workload_identity_pool_id = local.pool_id
  display_name              = "GitLab WIF Pool (${var.environment})"
  description               = "Workload Identity Pool for GitLab CI (${var.environment})"
}

# -------------------------------------------------------------------
# 2) Provider OIDC (GitLab.com)
# -------------------------------------------------------------------
resource "google_iam_workload_identity_pool_provider" "provider" {
  project                            = var.project_id
  workload_identity_pool_id          = google_iam_workload_identity_pool.pool.workload_identity_pool_id
  workload_identity_pool_provider_id = local.provider_id
  display_name                       = "GitLab OIDC Provider (${var.environment})"
  description                        = "OIDC provider for gitlab.com"

  oidc {
    issuer_uri = "https://gitlab.com"
  }

  # Mapping claims GitLab -> attributs IAM
  attribute_mapping = {
    "google.subject"         = "assertion.sub"
    "attribute.project_path" = "assertion.project_path"
    "attribute.ref"          = "assertion.ref"
  }

  # Sécurité :
  # - limite au projet GitLab
  # - + limite sur ref_prefix (tags/branches)
  attribute_condition = "attribute.project_path == \"${var.gitlab_project_path}\" && (startsWith(attribute.ref, \"refs/tags/${var.ref_prefix}\") || startsWith(attribute.ref, \"refs/heads/${var.ref_prefix}\"))"

  # ✅ IMPORTANT : empêche Terraform CI de tenter un update (403)
  lifecycle {
    ignore_changes = [
      attribute_condition
    ]
  }
}

# -------------------------------------------------------------------
# 3) Autoriser l'impersonation du Service Account
# roles/iam.workloadIdentityUser
# -------------------------------------------------------------------
resource "google_service_account_iam_member" "wif_impersonation" {
  service_account_id = local.service_account_full_id
  role               = "roles/iam.workloadIdentityUser"
  member             = local.principal_project
}

# -------------------------------------------------------------------
# 4) Autoriser la création de tokens temporaires (STS)
# roles/iam.serviceAccountTokenCreator
# -------------------------------------------------------------------
resource "google_service_account_iam_member" "token_creator" {
  service_account_id = local.service_account_full_id
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = local.principal_project
}