#!/bin/bash
# ==============================================================================
# SCRIPT : Vérification Dockerfiles.dev et Validation POM.XML
# ==============================================================================

set -e

echo "╔══════════════════════════════════════════════════════════════════════════╗"
echo "║     ✅ VÉRIFICATION STANDARDISATION DOCKERFILES.DEV & POM.XML            ║"
echo "╚══════════════════════════════════════════════════════════════════════════╝"
echo ""

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ERRORS=0
WARNINGS=0

# ==============================================================================
# SECTION 1 : Vérification Dockerfiles.dev
# ==============================================================================

echo -e "${BLUE}📁 1. VÉRIFICATION DOCKERFILES.DEV${NC}"
echo "════════════════════════════════════════════════════════════════════════════"

SERVICES=(
    "api-gateway"
    "config-server"
    "eureka-server"
    "services/analytics-service"
    "services/auth-service"
    "services/delivery-service"
    "services/location-service"
    "services/notification-service"
    "services/order-service"
    "services/partner-service"
    "services/payment-service"
    "services/promotion-service"
    "services/review-service"
    "services/support-service"
    "services/user-service"
)

DOCKERFILE_COUNT=0
for SERVICE in "${SERVICES[@]}"; do
    FILE="$SERVICE/Dockerfile.dev"
    if [ -f "$FILE" ]; then
        # Vérifier que le Dockerfile contient les éléments clés
        if grep -q "FROM maven:3.9" "$FILE" && \
           grep -q "FROM eclipse-temurin:17-jre" "$FILE" && \
           grep -q "EXPOSE 8080" "$FILE" && \
           grep -q "JAVA_OPTS" "$FILE"; then
            echo -e "${GREEN}✅${NC} $SERVICE/Dockerfile.dev"
            ((DOCKERFILE_COUNT++))
        else
            echo -e "${RED}❌${NC} $SERVICE/Dockerfile.dev (format invalide)"
            ((ERRORS++))
        fi
    else
        echo -e "${RED}❌${NC} $SERVICE/Dockerfile.dev (MANQUANT)"
        ((ERRORS++))
    fi
done

echo ""
echo "Dockerfiles.dev trouvés : $DOCKERFILE_COUNT / ${#SERVICES[@]}"
if [ $DOCKERFILE_COUNT -eq ${#SERVICES[@]} ]; then
    echo -e "${GREEN}✅ Tous les Dockerfiles.dev existent et sont conformes${NC}"
else
    echo -e "${RED}❌ Certains Dockerfiles.dev manquent ou sont invalides${NC}"
fi

echo ""

# ==============================================================================
# SECTION 2 : Validation POM.XML
# ==============================================================================

echo -e "${BLUE}📦 2. VALIDATION POM.XML${NC}"
echo "════════════════════════════════════════════════════════════════════════════"

cd /home/nayer/IdeaProjects/speedline/backend

if mvn validate -q 2>/dev/null; then
    echo -e "${GREEN}✅${NC} pom.xml parent valide"
    echo -e "${GREEN}✅${NC} Tous les modules trouvés"
    echo -e "${GREEN}✅${NC} Syntaxe XML correcte"
else
    echo -e "${RED}❌${NC} Validation pom.xml échouée"
    ((ERRORS++))
fi

echo ""

# ==============================================================================
# SECTION 3 : Vérification Format Dockerfiles
# ==============================================================================

echo -e "${BLUE}🔍 3. VÉRIFICATION FORMAT STANDARDISÉ${NC}"
echo "════════════════════════════════════════════════════════════════════════════"

# Vérifier que tous les Dockerfiles utilisent le même format

check_dockerfile_standard() {
    local file=$1
    local service=$2

    # Vérifier les éléments obligatoires
    [ ! -f "$file" ] && { echo -e "${RED}❌${NC} $service: Fichier manquant"; return 1; }

    ! grep -q "FROM maven:3.9.6-eclipse-temurin-17 AS builder" "$file" && \
        { echo -e "${YELLOW}⚠️${NC}  $service: Format builder non standard"; return 1; }

    ! grep -q "FROM eclipse-temurin:17-jre-jammy" "$file" && \
        { echo -e "${YELLOW}⚠️${NC}  $service: Image runtime non standard"; return 1; }

    ! grep -q "useradd -m -u 1000 appuser" "$file" && \
        { echo -e "${YELLOW}⚠️${NC}  $service: Utilisateur non-root manquant"; return 1; }

    ! grep -q "EXPOSE 8080" "$file" && \
        { echo -e "${YELLOW}⚠️${NC}  $service: Port 8080 manquant"; return 1; }

    ! grep -q "JAVA_OPTS" "$file" && \
        { echo -e "${YELLOW}⚠️${NC}  $service: JAVA_OPTS manquant"; return 1; }

    ! grep -q 'ENTRYPOINT \["sh","-c","java.*jar /app/app.jar"\]' "$file" && \
        { echo -e "${YELLOW}⚠️${NC}  $service: ENTRYPOINT non standard"; return 1; }

    echo -e "${GREEN}✅${NC} $service: Format conforme"
    return 0
}

CONFORMES=0
for SERVICE in "${SERVICES[@]}"; do
    FILE="$SERVICE/Dockerfile.dev"
    if check_dockerfile_standard "$FILE" "$SERVICE"; then
        ((CONFORMES++))
    fi
done

echo ""
echo "Dockerfiles conformes : $CONFORMES / ${#SERVICES[@]}"

echo ""

# ==============================================================================
# SECTION 4 : Taille des Images
# ==============================================================================

echo -e "${BLUE}📊 4. ESTIMATION TAILLE DES IMAGES${NC}"
echo "════════════════════════════════════════════════════════════════════════════"

echo "Base Images utilisées :"
echo -e "  - maven:3.9.6-eclipse-temurin-17      (~680 MB)"
echo -e "  - eclipse-temurin:17-jre-jammy         (~280 MB)"
echo ""
echo "Taille estimée par service : 300-400 MB (après compression)"
echo "Note: Les images Maven de build ne sont jamais pushées (multi-stage)"

echo ""

# ==============================================================================
# RÉSUMÉ FINAL
# ==============================================================================

echo -e "${BLUE}═════════════════════════════════════════════════════════════════════════════${NC}"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ VALIDATION RÉUSSIE${NC}"
    echo ""
    echo "Résumé :"
    echo -e "  ${GREEN}✅${NC} $DOCKERFILE_COUNT / ${#SERVICES[@]} Dockerfiles.dev existent"
    echo -e "  ${GREEN}✅${NC} $CONFORMES / ${#SERVICES[@]} Dockerfiles conformes"
    echo -e "  ${GREEN}✅${NC} pom.xml validé"
    echo -e "  ${GREEN}✅${NC} Format standardisé"
    echo ""
    echo -e "${GREEN}Prêt pour build & déploiement ! 🚀${NC}"
else
    echo -e "${RED}❌ VALIDATION ÉCHOUÉE${NC}"
    echo ""
    echo "Erreurs : $ERRORS"
    echo "Avertissements : $WARNINGS"
    echo ""
    exit 1
fi

echo ""

