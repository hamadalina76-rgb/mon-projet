# Infrastructure Terraform (GCP)

Structure:
- modules/: modules réutilisables (Artifact Registry, IAM, Cloud SQL, Pub/Sub, Cloud Run, Envoy)
- envs/: environnements (dev/staging/prod)
- scripts/: scripts utilitaires (validation, génération config Envoy)

Bonnes pratiques:
- Remote state dans GCS (bucket tfstate par environnement)
- Pas de secrets dans terraform.tfvars (préférer Secret Manager ou CI variables)
