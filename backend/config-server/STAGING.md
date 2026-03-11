# 📚 GUIDE STAGING - CONFIG SERVER SPEEDLINE

## 📋 Table des matières

1. [Configuration Initiale](#configuration-initiale)
2. [Démarrage du Service](#démarrage-du-service)
3. [Variables d'Environnement](#variables-denvironnement)
4. [Docker Staging](#docker-staging)
5. [Monitoring](#monitoring)
6. [Troubleshooting](#troubleshooting)

---

## 🚀 Configuration Initiale

### Prérequis
- **Java 17+** (OpenJDK 17)
- **Maven 3.8+**
- **Docker & Docker Compose** (recommandé)

### Vérifier l'installation Java
```bash
java -version
```

---

## ▶️ Démarrage du Service

### 1️⃣ Démarrage Local

```bash
source .env.staging
mvn clean package -DskipTests
SPRING_PROFILES_ACTIVE=staging java -jar target/config-server-1.0.0.jar
```

### 2️⃣ Démarrage avec Docker

```bash
source .env.staging

docker build -f Dockerfile.staging -t speedline/config-server:staging .

docker run -p 8888:8888 -p 9090:9090 \
  --env-file .env.staging \
  --name config-server-staging \
  speedline/config-server:staging

docker logs -f config-server-staging
```

### 3️⃣ Démarrage avec Docker Compose

```bash
docker-compose -f docker-compose.staging.yml up config-server
```

---

## 🔐 Variables d'Environnement

### Fichier `.env.staging`

| Variable | Valeur | Description |
|----------|--------|-------------|
| `SPRING_PROFILES_ACTIVE` | `staging` | Profil Spring actif |
| `CONFIG_USER` | `config-staging` | Utilisateur staging |
| `CONFIG_PASSWORD` | `config-staging-*` | Mot de passe staging |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | Docker URL | Eureka Server |
| `CONFIG_SERVER_SEARCH_LOCATIONS` | `classpath:/config` | Localisation config |
| `MANAGEMENT_SERVER_PORT` | `9090` | Port Actuator |

---

## 🐳 Docker Staging

### Builder l'image
```bash
docker build -f Dockerfile.staging -t speedline/config-server:staging .
```

### Lancer avec network
```bash
docker run -p 8888:8888 -p 9090:9090 \
  --network speedline-network \
  --env-file .env.staging \
  speedline/config-server:staging
```

---

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:9090/actuator/health
```

### Récupérer les configurations
```bash
curl -u config-staging:config-staging http://localhost:8888/api-gateway/staging
```

---

## 🔧 Troubleshooting

### Problème : Authentification échoue
```bash
# Vérifier les credentials staging
curl -u config-staging:config-staging-password http://localhost:8888/api-gateway/staging
```

### Problème : Config server inaccessible
```bash
docker logs config-server-staging | grep ERROR
```

---

**Dernière mise à jour** : 2026-02-23  
**Profil** : Staging (Pré-production)

