# ------------------------------------------------------------
# Backend distant Terraform (GCS)
# ------------------------------------------------------------
# Stocke le fichier terraform.tfstate dans Google Cloud Storage
# Permet :
# - Collaboration
# - Versioning
# - Sécurité
# - Compatibilité CI/CD
# ------------------------------------------------------------

terraform {
  backend "gcs" {
    bucket = "speedline-dev-488413-tfstate"
    prefix = "terraform/state"
  }
}