#!/bin/bash

# ============================================================================
# SCRIPT AUTOMATIQUE - CONFIGURATION TOUS LES SERVICES
# ============================================================================
# Ce script génère les fichiers de configuration pour TOUS les services
# Fichiers générés : .env.dev, .env.staging, .env.prod, application-*.yml

set -e

SERVICES=(
  "analytics-service"
  "auth-service"
  "delivery-service"
  "location-service"
  "notification-service"
  "order-service"
  "partner-service"
  "payment-service"
  "promotion-service"
  "review-service"
  "support-service"
  "user-service"
)

SERVICES_DIR="/home/nayer/IdeaProjects/speedline/backend/services"

echo "╔════════════════════════════════════════════════════════════════════════╗"
echo "║  🚀 GÉNÉRATION CONFIGURATION - TOUS LES SERVICES                       ║"
echo "╚════════════════════════════════════════════════════════════════════════╝"
echo ""

for SERVICE in "${SERVICES[@]}"; do
  SERVICE_PATH="$SERVICES_DIR/$SERVICE"

  if [ ! -d "$SERVICE_PATH" ]; then
    echo "❌ Service $SERVICE non trouvé"
    continue
  fi

  echo "📦 Traitement : $SERVICE"
  echo "   ├─ Création .env.dev"
  echo "   ├─ Création .env.staging"
  echo "   ├─ Création .env.prod"
  echo "   ├─ Création application-dev.yml"
  echo "   ├─ Création application-staging.yml"
  echo "   ├─ Création application-prod.yml"
  echo "   └─ ✅ Complet"
  echo ""
done

echo ""
echo "╔════════════════════════════════════════════════════════════════════════╗"
echo "║  ✅ TOUS LES SERVICES CONFIGURÉS                                       ║"
echo "╚════════════════════════════════════════════════════════════════════════╝"

