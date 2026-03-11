# 📚 GUIDE DÉVELOPPEMENT - ANALYTICS SERVICE

## 📋 Table des matières

1. [Configuration Initiale](#configuration-initiale)
2. [Démarrage du Service](#démarrage-du-service)
3. [Variables d'Environnement](#variables-denvironnement)
4. [Architecture](#architecture)
5. [Base de Données](#base-de-données)
6. [Cache Redis](#cache-redis)
7. [Monitoring](#monitoring)
8. [Endpoints](#endpoints)
9. [Troubleshooting](#troubleshooting)

---

## 🚀 Configuration Initiale

### Prérequis
- **Java 17+**
- **Maven 3.8+**
- **MySQL 8.0+**
- **Redis**
- **Docker & Docker Compose** (optionnel)

---

## ▶️ Démarrage du Service

### 1️⃣ Local (Sans Docker)

```bash
# Charger les variables
source .env.dev

# Compiler
mvn clean package -DskipTests

# Démarrer
SPRING_PROFILES_ACTIVE=dev java -jar target/analytics-service-1.0.0.jar
```

### 2️⃣ Docker

```bash
source .env.dev
docker build -f Dockerfile.dev -t speedline/analytics-service:dev .
docker run -p 8089:8089 --env-file .env.dev speedline/analytics-service:dev
```

### 3️⃣ Docker Compose

```bash
docker-compose -f docker-compose.dev.yml up analytics-service
```

---

## 🔐 Variables d'Environnement

### .env.dev

| Variable | Valeur Par Défaut | Description |
|----------|------------------|-------------|
| `SERVER_PORT` | `8089` | Port du service |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/analytics_dev` | Base de données |
| `SPRING_DATASOURCE_USERNAME` | `root` | Utilisateur BD |
| `SPRING_DATASOURCE_PASSWORD` | `root` | Mot de passe BD |
| `SPRING_REDIS_HOST` | `localhost` | Hôte Redis |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | `http://eureka:eureka123@localhost:8761/eureka/` | Eureka URL |

---

## 🗺️ Architecture

Le service analytics fournit :
- ✅ Génération de rapports
- ✅ Collecte de métriques
- ✅ Analyses et statistiques
- ✅ Cache des données

---

## 💾 Base de Données

### Créer la base de données

```bash
mysql -u root -p
CREATE DATABASE analytics_dev CHARACTER SET utf8mb4;
USE analytics_dev;
```

### Vérifier les migrations

```bash
curl http://localhost:9090/actuator/health
```

---

## 🔴 Cache Redis

```bash
# Vérifier la connexion
redis-cli ping
# PONG

# Afficher les clés
redis-cli KEYS "*analytics*"
```

---

## 📊 Monitoring

```bash
# Health check
curl http://localhost:9090/actuator/health

# Métriques
curl http://localhost:9090/actuator/metrics
```

---

## 🔗 Endpoints

```bash
# Vérifier l'enregistrement Eureka
curl -u eureka:eureka123 http://localhost:8761/eureka/apps/analytics-service
```

---

## 🔧 Troubleshooting

### Problème : Connexion BD échoue
```bash
# Vérifier MySQL
mysql -u root -p -e "SHOW DATABASES;"

# Vérifier la URL
echo $SPRING_DATASOURCE_URL
```

### Problème : Redis non accessible
```bash
# Vérifier Redis
redis-cli ping

# Ou lancer Redis
docker run -d -p 6379:6379 redis:latest
```

### Problème : Service non enregistré dans Eureka
```bash
# Vérifier les logs
docker logs analytics-service | grep -i eureka
```

---

## 🔔 Pub/Sub (Google Cloud) - Développement local

Le service `analytics-service` peut consommer ou publier des événements via Pub/Sub (optionnel).
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
GCP_PROJECT_ID=analytics-dev
PUBSUB_EMULATOR_HOST=localhost:8085
GOOGLE_APPLICATION_CREDENTIALS=${HOME}/.config/gcloud/application_default_credentials.json
```

### Créer un topic et tester
```bash
# Avec l'émulateur en cours
gcloud pubsub topics create analytics-events --project=analytics-dev
gcloud pubsub topics publish analytics-events --message='{"e":"test"}' --project=analytics-dev
```

### Notes
- En production, Pub/Sub est géré par GCP et les credentials sont fournis par le Service Account attaché au service (Cloud Run).
- L'ajout ci-dessus est optionnel : si `analytics-service` n'utilise pas Pub/Sub, ces valeurs n'ont pas d'effet.

---

**Version** : 1.0.0  
**Port** : 8089

