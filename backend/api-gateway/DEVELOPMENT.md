# 📚 GUIDE DÉVELOPPEMENT - API GATEWAY SPEEDLINE
## 📋 Table des matières
1. [Configuration Initiale](#configuration-initiale)
2. [Démarrage du Service](#démarrage-du-service)
3. [Variables d'Environnement](#variables-denvironnement)
4. [Fichiers de Configuration](#fichiers-de-configuration)
5. [Architecture des Routes](#architecture-des-routes)
6. [Debugging & Logs](#debugging--logs)
7. [Vérification de la Santé](#vérification-de-la-santé)
8. [Docker Development](#docker-development)
9. [Troubleshooting](#troubleshooting)
---
## 🚀 Configuration Initiale
### Prérequis
- **Java 17+** (OpenJDK 17)
- **Maven 3.8+**
- **Docker & Docker Compose** (optionnel mais recommandé)
- **Git**
### Vérifier l'installation Java
\`\`\`bash
java -version
# Résultat attendu : openjdk version "17.x.x" ou supérieur
\`\`\`
### Cloner et compiler le projet
\`\`\`bash
cd /home/nayer/IdeaProjects/speedline/backend
mvn clean compile
\`\`\`
---
## ▶️ Démarrage du Service
### 1️⃣ Démarrage Local (Sans Docker)
#### Option A : Avec IDE IntelliJ IDEA
\`\`\`
1. Ouvrir le projet dans IntelliJ
2. Edit Configurations → Add Configuration
3. Ajouter VM options:
   -Dspring.profiles.active=dev
4. Ajouter Environment variables depuis .env.dev
5. Run → Run 'ApiGatewayApplication'
\`\`\`
#### Option B : Ligne de commande
\`\`\`bash
# Charger les variables d'environnement
source .env.dev
# Compiler le projet
mvn clean package -DskipTests
# Démarrer l'application
SPRING_PROFILES_ACTIVE=dev java -jar target/api-gateway-1.0.0.jar
\`\`\`
### 2️⃣ Démarrage avec Docker
\`\`\`bash
# Charger les variables
source .env.dev
# Builder l'image dev
docker build -f Dockerfile.dev -t speedline/api-gateway:dev .
# Lancer le conteneur
docker run -p 8080:8080 -p 9090:9090 \
  --env-file .env.dev \
  --name api-gateway-dev \
  speedline/api-gateway:dev
# Afficher les logs
docker logs -f api-gateway-dev
\`\`\`
### 3️⃣ Démarrage avec Docker Compose
\`\`\`bash
# À la racine du backend
docker-compose up api-gateway
# Avec logs
docker-compose up -d api-gateway && docker-compose logs -f api-gateway
\`\`\`
---
## 🔐 Variables d'Environnement
### Fichier \`.env.dev\`
Ce fichier contient **TOUTES** les variables d'environnement pour le développement.
#### Variables Principales
| Variable | Valeur Par Défaut | Description |
|----------|------------------|-------------|
| \`SPRING_PROFILES_ACTIVE\` | \`dev\` | Profil Spring actif |
| \`REDIS_HOST\` | \`localhost\` | Hôte Redis (use \`redis\` en Docker) |
| \`REDIS_PORT\` | \`6379\` | Port Redis |
| \`EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE\` | \`http://eureka:eureka123@localhost:8761/eureka/\` | URL Eureka Server |
| \`JWT_SECRET\` | (clé longue) | Clé de signature JWT |
| \`JWT_EXPIRATION\` | \`3600\` | Durée token en secondes (1h) |
| \`MANAGEMENT_SERVER_PORT\` | \`9090\` | Port Actuator |
| \`CORS_ALLOWED_ORIGINS\` | \`http://localhost:*\` | Origines CORS autorisées |
#### Sourcer les variables (Linux/Mac)
\`\`\`bash
source .env.dev
echo \$REDIS_HOST  # Vérifier
\`\`\`
---
## ⚙️ Fichiers de Configuration
### Structure
\`\`\`
api-gateway/
├── Dockerfile              # Production (default)
├── Dockerfile.dev          # Développement
├── Dockerfile.staging      # Staging/Pré-prod
├── Dockerfile.prod         # Production optimisée
├── .env.dev               # Variables dev
├── pom.xml                # Dépendances Maven
└── src/main/resources/
    ├── application.yml                # Config par défaut
    ├── application-dev.yml           # ✨ Config développement
    ├── application-staging.yml       # Config staging
    └── application-prod.yml          # Config production
\`\`\`
### application.yml (Configuration de base)
- Routes vers tous les microservices
- Configuration Eureka par défaut
- Configuration Redis par défaut
### application-dev.yml (Configuration développement)
✅ **AJOUTÉ** avec :
- Logs DEBUG activés
- Lazy initialization (démarrage plus rapide)
- Health checks complets
- DevTools activés
- Circuit Breaker en dev
---
## 🗺️ Architecture des Routes
L'API Gateway route les requêtes vers les microservices selon les patterns URL :
### Routes Principales
\`\`\`
┌─ /api/v1/auth/**          → auth-service (Authentification)
├─ /api/v1/admins/**        → user-service (Gestion admins)
├─ /api/users/**            → user-service (Utilisateurs)
├─ /api/partners/**         → partner-service (Partenaires)
├─ /api/orders/**           → order-service (Commandes)
├─ /api/deliveries/**       → delivery-service (Livraisons)
├─ /api/payments/**         → payment-service (Paiements)
├─ /api/notifications/**    → notification-service (Notifications)
├─ /api/locations/**        → location-service (Localisation)
├─ /api/analytics/**        → analytics-service (Analytiques)
├─ /api/promotions/**       → promotion-service (Promotions)
├─ /api/reviews/**          → review-service (Avis)
└─ /api/tickets/**          → support-service (Support)
\`\`\`
### Filtres
| Filtre | Fonctionnalité |
|--------|---------------|
| \`StripPrefix=1\` | Enlever \`/api\` avant router |
| \`AuthenticationFilter\` | Vérifier le JWT |
| \`DedupeResponseHeader\` | Éviter les headers CORS dupliqués |
---
## 🐛 Debugging & Logs
### Activer les logs DEBUG
\`\`\`bash
# Dans application-dev.yml :
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    com.speedline: DEBUG
\`\`\`
### Afficher les logs en temps réel
**IDE IntelliJ** :
- Console affiche les logs automatiquement
- Filtrer par : "ERROR", "WARN", etc.
**Docker** :
\`\`\`bash
docker logs -f api-gateway-dev
\`\`\`
**Local** :
\`\`\`bash
tail -f logs/api-gateway.log
\`\`\`
### Debugging Java à distance
\`\`\`bash
# Démarrer avec debug port
SPRING_PROFILES_ACTIVE=dev java \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar target/api-gateway-1.0.0.jar
# Dans IDE : Run → Edit Configurations → Remote → Port 5005
\`\`\`
---
## ✅ Vérification de la Santé
### Health Check (Port 9090)
\`\`\`bash
curl http://localhost:9090/actuator/health
# Réponse : {"status":"UP"}
curl http://localhost:9090/actuator/health/readiness
# Réponse : {"status":"UP"} ou détails complets
\`\`\`
### Afficher les routes configurées
\`\`\`bash
curl http://localhost:9090/actuator/gateway/routes | jq
\`\`\`
### Afficher les métriques
\`\`\`bash
curl http://localhost:9090/actuator/metrics | jq
curl http://localhost:9090/actuator/prometheus
\`\`\`
### Afficher les informations d'application
\`\`\`bash
curl http://localhost:9090/actuator/info | jq
\`\`\`
---
## 🐳 Docker Development
### Builder l'image dev
\`\`\`bash
docker build -f Dockerfile.dev -t speedline/api-gateway:dev .
\`\`\`
### Lancer le conteneur avec volumes
\`\`\`bash
docker run -p 8080:8080 -p 9090:9090 \
  -v \$(pwd)/src:/app/src \
  -v \$(pwd)/logs:/app/logs \
  --env-file .env.dev \
  speedline/api-gateway:dev
\`\`\`
### Exécuter des commandes dans le conteneur
\`\`\`bash
docker exec -it api-gateway-dev sh
ls -la /app
cat /app/app.jar
\`\`\`
### Afficher les logs en temps réel
\`\`\`bash
docker logs -f api-gateway-dev
# Afficher les 100 dernières lignes
docker logs --tail 100 api-gateway-dev
\`\`\`
---
## 🔧 Troubleshooting
### Problème : Port 8080 déjà utilisé
\`\`\`bash
# Trouver le processus utilisant le port
lsof -i :8080
# Tuer le processus
kill -9 <PID>
# Ou utiliser un autre port
export SERVER_PORT=8081
\`\`\`
### Problème : Redis non disponible
\`\`\`bash
# Vérifier que Redis est lancé
docker ps | grep redis
# Lancer Redis
docker run -d -p 6379:6379 redis:7-alpine
# Ou via docker-compose
docker-compose up -d redis
\`\`\`
### Problème : Eureka non trouvé
\`\`\`bash
# Vérifier Eureka
curl http://localhost:8761/eureka/apps
# Si localhost ne marche pas, utiliser le conteneur
# Éditer .env.dev :
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:eureka123@eureka:8761/eureka/
\`\`\`
### Problème : Application démarre mais routes ne marchent pas
\`\`\`bash
# Vérifier les logs
curl http://localhost:9090/actuator/gateway/routes
# Vérifier les erreurs
docker logs api-gateway-dev | grep ERROR
\`\`\`
### Problème : JWT token rejeté
\`\`\`bash
# Vérifier la clé secrète
echo \$JWT_SECRET
# Vérifier les logs d'authentification
docker logs api-gateway-dev | grep -i auth
\`\`\`
---
## 🔔 Pub/Sub (Google Cloud) - Développement local

L'API Gateway peut publier des événements vers Pub/Sub (utilisation optionnelle).
Pour le développement local, on utilise l'émulateur Pub/Sub.

### Démarrer l'émulateur Pub/Sub
```bash
# Démarrer l'émulateur (gcloud doit être installé)
gcloud beta emulators pubsub start --host-port=localhost:8085 &

# Exporter la variable d'environnement
export PUBSUB_EMULATOR_HOST=localhost:8085
```

### Configurer .env.dev
Assurez-vous d'avoir ces variables dans `.env.dev` :
```env
GCP_PROJECT_ID=api-gateway-dev
PUBSUB_EMULATOR_HOST=localhost:8085
GOOGLE_APPLICATION_CREDENTIALS=${HOME}/.config/gcloud/application_default_credentials.json
```

### Créer un topic pour tester
```bash
# Avec l'émulateur en cours
gcloud pubsub topics create api-gateway-events --project=api-gateway-dev

gcloud pubsub topics publish api-gateway-events --message='{"event":"test"}' --project=api-gateway-dev
```

### Notes
- En production, Pub/Sub est géré par GCP et les credentials sont fournis par le Service Account attaché à Cloud Run.
- L'API Gateway n'utilise Pub/Sub que si vous implémentez des publishers; la configuration ci-dessus permet l'utilisation depuis le gateway.
---
## 📝 Checklist Démarrage
- [ ] Java 17+ installé
- [ ] Maven 3.8+ installé
- [ ] Fichier \`.env.dev\` existe
- [ ] Redis lancé (\`docker run -d redis\`)
- [ ] Eureka Server lancé
- [ ] Autres microservices registrés dans Eureka
- [ ] Compiler : \`mvn clean package\`
- [ ] Démarrer l'API Gateway
- [ ] Vérifier santé : \`curl http://localhost:9090/actuator/health\`
- [ ] Tester une route : \`curl http://localhost:8080/api/v1/auth/...\`
---
## 📚 Ressources
- **Spring Cloud Gateway** : https://spring.io/projects/spring-cloud-gateway
- **Spring Cloud Eureka** : https://spring.io/projects/spring-cloud-eureka
- **Spring Boot Actuator** : https://spring.io/guides/gs/actuator-service/
- **JWT Auth** : https://jwt.io/
- **Docker Docs** : https://docs.docker.com/
---
**Dernière mise à jour** : 2026-02-23  
**Auteur** : GitHub Copilot  
**Version** : 1.0.0
