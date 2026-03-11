#!/bin/bash
# ==============================================================================
# SCRIPT : Activation des APIs GCP requises pour Speedline
# ==============================================================================
# Usage: ./activate-apis.sh <PROJECT_ID>
# ==============================================================================

set -e

PROJECT_ID="${1:-}"

if [ -z "$PROJECT_ID" ]; then
  echo "❌ Erreur : PROJECT_ID requis"
  echo "Usage: $0 <PROJECT_ID>"
  exit 1
fi

echo "🚀 Activation des APIs GCP pour le projet: $PROJECT_ID"
echo "════════════════════════════════════════════════════════"

# Liste des APIs à activer
APIS=(
  "cloudresourcemanager.googleapis.com"
  "iam.googleapis.com"
  "compute.googleapis.com"
  "run.googleapis.com"
  "artifactregistry.googleapis.com"
  "sqladmin.googleapis.com"
  "pubsub.googleapis.com"
  "redis.googleapis.com"
  "vpcaccess.googleapis.com"
  "servicenetworking.googleapis.com"
  "cloudapis.googleapis.com"
  "logging.googleapis.com"
  "monitoring.googleapis.com"
  "secretmanager.googleapis.com"
  "iamcredentials.googleapis.com"
  "sts.googleapis.com"
)

# Activation des APIs
for API in "${APIS[@]}"; do
  echo "📦 Activation de: $API"
  gcloud services enable "$API" --project="$PROJECT_ID" 2>/dev/null && \
    echo "✅ $API activée" || \
    echo "⚠️  $API déjà activée ou erreur"
done

echo ""
echo "════════════════════════════════════════════════════════"
echo "✅ Activation terminée !"
echo "⏱️  Attendre 60 secondes pour la propagation..."
sleep 60
echo "✅ Prêt pour Terraform apply"

