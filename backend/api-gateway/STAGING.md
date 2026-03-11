# 📚 GUIDE STAGING - API GATEWAY SPEEDLINE

## 📋 Table des matières

1. [Configuration Initiale](#configuration-initiale)
2. [Démarrage du Service](#démarrage-du-service)
3. [Variables d'Environnement](#variables-denvironnement)
4. [Fichiers de Configuration](#fichiers-de-configuration)
5. [Architecture des Routes](#architecture-des-routes)
6. [Monitoring & Logs](#monitoring--logs)
7. [Vérification de la Santé](#vérification-de-la-santé)
8. [Docker Staging](#docker-staging)
9. [Troubleshooting](#troubleshooting)

---

## 🚀 Configuration Initiale

### Prérequis
- **Java 17+** (OpenJDK 17)
- **Maven 3.8+**
- **Docker & Docker Compose** (recommandé)
- **Git**

### Vérifier l'installation Java
```bash
java -version
# Résultat attendu : openjdk version "17.x.x" ou supérieur
```

### Cloner et compiler le projet
```bash
cd /home/nayer/IdeaProjects/speedline/backend
mvn clean compile
```

---

## ▶️ Démarrage du Service

### 1️⃣ Démarrage Local (Sans Docker)

#### Option A : Avec IDE IntelliJ IDEA
```
1. Ouvrir le projet dans IntelliJ
2. Edit Configurations → Add Configuration
3. Ajouter VM options:
   -Dspring.profiles.active=staging
4. Ajouter Environment variables depuis .env.staging
5. Run → Run 'ApiGatewayApplication'
```

#### Option B : Ligne de commande
```bash
# Charger les variables d'environnement
source .env.staging

# Compiler le projet
mvn clean package -DskipTests

# Démarrer l'application
SPRING_PROFILES_ACTIVE=staging java -jar target/api-gateway-1.0.0.jar
```

### 2️⃣ Démarrage avec Docker

```bash
# Charger les variables
source .env.staging

# Builder l'image staging
docker build -f Dockerfile.staging -t speedline/api-gateway:staging .

# Lancer le conteneur
docker run -p 8080:8080 -p 9090:9090 \
  --env-file .env.staging \
  --name api-gateway-staging \
  speedline/api-gateway:staging

# Afficher les logs
docker logs -f api-gateway-staging
```

### 3️⃣ Démarrage avec Docker Compose

```bash
# À la racine du backend
docker-compose -f docker-compose.staging.yml up api-gateway

# Avec logs
docker-compose -f docker-compose.staging.yml up -d api-gateway && \
docker-compose -f docker-compose.staging.yml logs -f api-gateway
```

---

## 🔐 Variables d'Environnement

### Fichier `.env.staging`

Ce fichier contient toutes les variables d'environnement pour le staging (pré-production).

#### Variables Principales

| Variable | Valeur Par Défaut | Description |
|----------|------------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `staging` | Profil Spring actif |
| `REDIS_HOST` | `redis` | Hôte Redis (conteneur Docker) |
| `REDIS_PORT` | `6379` | Port Redis |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | `http://eureka:eureka123@eureka:8761/eureka/` | URL Eureka Server |
| `JWT_SECRET` | (clé longue) | Clé de signature JWT |
| `MANAGEMENT_SERVER_PORT` | `9090` | Port Actuator |

#### Sourcer les variables (Linux/Mac)
```bash
source .env.staging
echo $REDIS_HOST  # Vérifier
```

---

## ⚙️ Fichiers de Configuration

### Structure
```
api-gateway/
├── Dockerfile              # Production (default)
├── Dockerfile.dev          # Développement
├── Dockerfile.staging      # Staging/Pré-prod
├── Dockerfile.prod         # Production optimisée
├── .env.staging           # Variables staging
├── pom.xml                # Dépendances Maven
└── src/main/resources/
    ├── application.yml                # Config par défaut
    ├── application-dev.yml           # Config développement
    ├── application-staging.yml       # Config staging (COURANT)
    └── application-prod.yml          # Config production
```

### application.yml (Configuration de base)
- Routes vers tous les microservices
- Configuration Eureka par défaut
- Configuration Redis par défaut

### application-staging.yml (Configuration staging)
✅ Configuration pour **pré-production** :
- Logs INFO (moins verbose que dev)
- Lazy initialization : false (production-like)
- Health checks avec restrictions
- Circuit Breaker plus strict que dev
- Ressources moyennes (300 threads max)

---

## 🗺️ Architecture des Routes

L'API Gateway route les requêtes vers les microservices selon les patterns URL :

### Routes Principales

```
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
```

### Filtres

| Filtre | Fonctionnalité |
|--------|---------------|
| `StripPrefix=1` | Enlever `/api` avant router |
| `AuthenticationFilter` | Vérifier le JWT |
| `DedupeResponseHeader` | Éviter les headers CORS dupliqués |

---

## 📊 Monitoring & Logs

### Configuration de Logging
```bash
# Dans application-staging.yml :
logging:
  level:
    root: WARN
    org.springframework: INFO
    org.springframework.cloud.gateway: INFO
    com.speedline: INFO
```

### Afficher les logs en temps réel

**IDE IntelliJ** :
- Console affiche les logs automatiquement
- Filtrer par : "ERROR", "WARN"

**Docker** :
```bash
docker logs -f api-gateway-staging
```

**Local** :
```bash
tail -f logs/api-gateway.log
```

### Monitoring des métriques
```bash
# Accéder à Prometheus (si configuré)
curl http://localhost:9090/actuator/prometheus

# Exporter les métriques
curl http://localhost:9090/actuator/metrics > metrics.json
```

---

## ✅ Vérification de la Santé

### Health Check (Port 9090)
```bash
curl http://localhost:9090/actuator/health
# Réponse : {"status":"UP"}

curl http://localhost:9090/actuator/health/readiness
# Réponse : {"status":"UP"} ou détails complets
```

### Afficher les routes configurées
```bash
curl http://localhost:9090/actuator/gateway/routes | jq
```

### Afficher les métriques
```bash
curl http://localhost:9090/actuator/metrics | jq
curl http://localhost:9090/actuator/metrics/http.server.requests
```

### Afficher les informations d'application
```bash
curl http://localhost:9090/actuator/info | jq
```

---

## 🐳 Docker Staging

### Builder l'image staging
```bash
docker build -f Dockerfile.staging -t speedline/api-gateway:staging .
```

### Lancer le conteneur avec networks
```bash
docker network create speedline-network 2>/dev/null || true

docker run -p 8080:8080 -p 9090:9090 \
  --network speedline-network \
  --env-file .env.staging \
  speedline/api-gateway:staging
```

### Exécuter des commandes dans le conteneur
```bash
docker exec -it api-gateway-staging sh
ls -la /app
```

### Afficher les logs en temps réel
```bash
docker logs -f api-gateway-staging

# Afficher les 100 dernières lignes
docker logs --tail 100 api-gateway-staging
```

---

## 🔧 Troubleshooting

### Problème : Port 8080 déjà utilisé
```bash
# Trouver le processus utilisant le port
lsof -i :8080

# Tuer le processus
kill -9 <PID>

# Ou utiliser un autre port
export SERVER_PORT=8081
```

### Problème : Redis non disponible
```bash
# Vérifier que Redis est lancé
docker ps | grep redis

# Lancer Redis pour staging
docker run -d --name redis-staging \
  -p 6379:6379 \
  redis:7-alpine
```

### Problème : Eureka non trouvé
```bash
# Vérifier Eureka
curl http://localhost:8761/eureka/apps

# Lancer Eureka Server
docker run -d --name eureka-server \
  -p 8761:8761 \
  eureka-server:latest
```

### Problème : Application démarre mais routes ne marchent pas
```bash
# Vérifier les logs
curl http://localhost:9090/actuator/gateway/routes

# Vérifier les erreurs
docker logs api-gateway-staging | grep ERROR
```

### Problème : Health check échoue
```bash
# Vérifier tous les composants
curl http://localhost:9090/actuator/health/liveness
curl http://localhost:9090/actuator/health/readiness

# Vérifier les détails
curl http://localhost:9090/actuator/health?full=true
```

---

## 📝 Checklist Déploiement

- [ ] Java 17+ installé
- [ ] Maven 3.8+ installé
- [ ] Fichier `.env.staging` existe et est configuré
- [ ] Redis lancé et accessible
- [ ] Eureka Server lancé et accessible
- [ ] Autres microservices registrés dans Eureka
- [ ] Compiler : `mvn clean package`
- [ ] Builder Docker : `docker build -f Dockerfile.staging -t speedline/api-gateway:staging .`
- [ ] Démarrer le conteneur
- [ ] Vérifier santé : `curl http://localhost:9090/actuator/health`
- [ ] Tester une route : `curl http://localhost:8080/api/v1/auth/...`

---

## 📚 Ressources

- **Spring Cloud Gateway** : https://spring.io/projects/spring-cloud-gateway
- **Spring Cloud Eureka** : https://spring.io/projects/spring-cloud-eureka
- **Spring Boot Actuator** : https://spring.io/guides/gs/actuator-service/
- **Docker Docs** : https://docs.docker.com/
- **Docker Compose** : https://docs.docker.com/compose/

---

**Dernière mise à jour** : 2026-02-23  
**Auteur** : GitHub Copilot  
**Version** : 1.0.0  
**Profil** : Staging (Pré-production)

