#!/bin/bash
# ==============================================================================
# SCRIPT : Attribution des permissions IAM au Service Account Terraform
# ==============================================================================
# Usage: ./grant-terraform-sa-permissions.sh <PROJECT_ID> <SA_EMAIL>
# ==============================================================================

set -e

PROJECT_ID="${1:-}"
SA_EMAIL="${2:-}"

if [ -z "$PROJECT_ID" ] || [ -z "$SA_EMAIL" ]; then
  echo "❌ Erreur : Arguments requis"
  echo "Usage: $0 <PROJECT_ID> <SA_EMAIL>"
  echo ""
  echo "Exemple:"
  echo "  $0 speedline-dev-460016 gitlab-ci-sa-dev@speedline-dev-460016.iam.gserviceaccount.com"
  exit 1
fi

echo "🔐 Attribution des permissions IAM pour Terraform"
echo "════════════════════════════════════════════════════════"
echo "Projet   : $PROJECT_ID"
echo "SA Email : $SA_EMAIL"
echo "════════════════════════════════════════════════════════"
echo ""

# Liste des rôles nécessaires
ROLES=(
  "roles/editor"                          # Gestion ressources générales
  "roles/iam.securityAdmin"               # Gestion IAM policies
  "roles/iam.serviceAccountAdmin"         # Gestion Service Accounts
  "roles/iam.workloadIdentityPoolAdmin"   # Gestion WIF
  "roles/serviceusage.serviceUsageAdmin"  # Activation APIs
  "roles/pubsub.admin"                    # Gestion Pub/Sub
  "roles/cloudsql.admin"                  # Gestion Cloud SQL
  "roles/redis.admin"                     # Gestion Redis
  "roles/vpcaccess.admin"                 # Gestion VPC Access
)

# Attribution des rôles
for ROLE in "${ROLES[@]}"; do
  echo "📌 Attribution du rôle: $ROLE"
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$SA_EMAIL" \
    --role="$ROLE" \
    --condition=None \
    --quiet 2>/dev/null && \
    echo "✅ $ROLE attribué" || \
    echo "⚠️  $ROLE déjà attribué ou erreur"
done

echo ""
echo "════════════════════════════════════════════════════════"
echo "✅ Attribution terminée !"
echo "⏱️  Attendre 60 secondes pour la propagation IAM..."
sleep 60
echo "✅ Prêt pour Terraform apply"
echo ""
echo "📝 Vérifier les permissions:"
echo "  gcloud projects get-iam-policy $PROJECT_ID \\"
echo "    --flatten=\"bindings[].members\" \\"
echo "    --filter=\"bindings.members:serviceAccount:$SA_EMAIL\""

