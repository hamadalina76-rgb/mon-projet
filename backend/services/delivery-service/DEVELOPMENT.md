# 📚 GUIDE DÉVELOPPEMENT - DELIVERY SERVICE

## 📋 Table des matières

1. [Configuration Initiale](#configuration-initiale)
2. [Démarrage du Service](#démarrage-du-service)
3. [Variables d'Environnement](#variables-denvironnement)
4. [Architecture](#architecture)
5. [Base de Données](#base-de-données)
6. [Kafka](#kafka)
7. [Monitoring](#monitoring)
8. [Endpoints](#endpoints)
9. [Troubleshooting](#troubleshooting)

---

## 🚀 Configuration Initiale

### Prérequis
- **Java 17+**
- **Maven 3.8+**
- **PostgreSQL 12+**
- **Kafka**
- **Docker & Docker Compose** (optionnel)

### Vérifier l'installation
```bash
java -version
mvn -version
psql --version
```

---

## ▶️ Démarrage du Service

### 1️⃣ Local (Sans Docker)

```bash
# Charger les variables
source .env.dev

# Compiler
mvn clean package -DskipTests

# Démarrer
SPRING_PROFILES_ACTIVE=dev java -jar target/delivery-service-1.0.0.jar
```

### 2️⃣ Docker

```bash
source .env.dev
docker build -f Dockerfile.dev -t speedline/delivery-service:dev .
docker run -p 8085:8085 -p 9090:9090 --env-file .env.dev speedline/delivery-service:dev
```

### 3️⃣ Docker Compose

```bash
docker-compose -f docker-compose.dev.yml up delivery-service
```

---

## 🔐 Variables d'Environnement

### .env.dev

| Variable | Valeur Par Défaut | Description |
|----------|------------------|-------------|
| `SERVER_PORT` | `8085` | Port du service |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/speedline_delivery` | Base de données |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Utilisateur BD |
| `SPRING_DATASOURCE_PASSWORD` | `postgres123` | Mot de passe BD |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Serveur Kafka |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | `http://eureka:eureka123@localhost:8761/eureka/` | Eureka URL |

---

## 🗺️ Architecture

Le service delivery gère :
- ✅ Création et suivi des livraisons
- ✅ Attribution des livreurs
- ✅ Tracking en temps réel
- ✅ Calcul des distances et temps
- ✅ Historique des livraisons

---

## 💾 Base de Données

### Créer la base de données

```bash
psql -U postgres
CREATE DATABASE speedline_delivery;
\c speedline_delivery
```

### Vérifier les migrations Flyway

```bash
# Les migrations s'exécutent automatiquement au démarrage
# Vérifier dans la BD :
SELECT * FROM flyway_schema_history;
```

---

## 📨 Kafka

### Topics utilisés

- `delivery.created` - Nouvelle livraison créée
- `delivery.assigned` - Livraison assignée à un livreur
- `delivery.picked-up` - Commande récupérée
- `delivery.delivered` - Livraison terminée
- `delivery.cancelled` - Livraison annulée

### Tester Kafka

```bash
# Produire un message
kafka-console-producer --broker-list localhost:9092 --topic delivery.created

# Consommer des messages
kafka-console-consumer --bootstrap-server localhost:9092 --topic delivery.created --from-beginning
```

---

## 📊 Monitoring

```bash
# Health check
curl http://localhost:9090/actuator/health

# Métriques
curl http://localhost:9090/actuator/metrics

# Info
curl http://localhost:9090/actuator/info
```

---

## 🔗 Endpoints

### API Delivery

```bash
# Créer une livraison
POST /api/deliveries

# Récupérer une livraison
GET /api/deliveries/{id}

# Lister les livraisons
GET /api/deliveries

# Assigner un livreur
PUT /api/deliveries/{id}/assign

# Mettre à jour le statut
PUT /api/deliveries/{id}/status
```

### Vérifier Eureka

```bash
curl -u eureka:eureka123 http://localhost:8761/eureka/apps/delivery-service
```

---

## 🔧 Troubleshooting

### Problème : Connexion BD échoue
```bash
# Vérifier PostgreSQL
psql -U postgres -c "SELECT version();"

# Vérifier la BD
psql -U postgres -c "\l" | grep speedline_delivery
```

### Problème : Kafka non accessible
```bash
# Vérifier Kafka
docker ps | grep kafka

# Ou lancer Kafka
docker run -d -p 9092:9092 --name kafka confluentinc/cp-kafka:latest
```

### Problème : Service non enregistré dans Eureka
```bash
# Vérifier les logs
docker logs delivery-service | grep -i eureka

# Vérifier la connexion
curl http://localhost:8761/
```

---

**Version** : 1.0.0  
**Port** : 8085  
**Profil** : Développement

