# 📚 GUIDE DÉVELOPPEMENT - EUREKA SERVER SPEEDLINE

## 📋 Table des matières

1. [Configuration Initiale](#configuration-initiale)
2. [Démarrage du Service](#démarrage-du-service)
3. [Variables d'Environnement](#variables-denvironnement)
4. [Architecture Eureka](#architecture-eureka)
5. [Enregistrement des Services](#enregistrement-des-services)
6. [Dashboard Eureka](#dashboard-eureka)
7. [Vérification de la Santé](#vérification-de-la-santé)
8. [Docker Development](#docker-development)
9. [Troubleshooting](#troubleshooting)
10. [Note Pub/Sub & GCP](#note-pubsub--gcp)

---

## 🚀 Configuration Initiale

### Prérequis
- **Java 17+** (OpenJDK 17)
- **Maven 3.8+**
- **Docker & Docker Compose** (optionnel)
- **Git**

### Vérifier l'installation Java
```bash
java -version
# Résultat attendu : openjdk version "17.x.x" ou supérieur
```

---

## ▶️ Démarrage du Service

### 1️⃣ Démarrage Local (Sans Docker)

#### Option A : Avec IDE IntelliJ IDEA
```
1. Ouvrir le projet dans IntelliJ
2. Edit Configurations → Add Configuration
3. Ajouter VM options:
   -Dspring.profiles.active=dev
4. Ajouter Environment variables depuis .env.dev
5. Run → Run 'EurekaServerApplication'
```

#### Option B : Ligne de commande
```bash
# Charger les variables d'environnement
source .env.dev

# Compiler le projet
mvn clean package -DskipTests

# Démarrer l'application
SPRING_PROFILES_ACTIVE=dev java -jar target/eureka-server-1.0.0.jar
```

### 2️⃣ Démarrage avec Docker

```bash
# Charger les variables
source .env.dev

# Builder l'image dev
docker build -f Dockerfile.dev -t speedline/eureka-server:dev .

# Lancer le conteneur
docker run -p 8761:8761 -p 9090:9090 \
  --env-file .env.dev \
  --name eureka-server-dev \
  speedline/eureka-server:dev

# Afficher les logs
docker logs -f eureka-server-dev
```

### 3️⃣ Démarrage avec Docker Compose

```bash
# À la racine du backend
docker-compose up eureka-server

# Avec logs
docker-compose up -d eureka-server && docker-compose logs -f eureka-server
```

---

## 🔐 Variables d'Environnement

### Fichier `.env.dev`

| Variable | Valeur Par Défaut | Description |
|----------|------------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Profil Spring actif |
| `EUREKA_USER` | `eureka` | Utilisateur Eureka |
| `EUREKA_PASSWORD` | `eureka123` | Mot de passe Eureka |
| `EUREKA_INSTANCE_HOSTNAME` | `localhost` | Hostname Eureka |
| `EUREKA_SERVER_ENABLE_SELF_PRESERVATION` | `false` | Désactiver en dev pour test rapides |
| `EUREKA_SERVER_EVICTION_INTERVAL_TIMER_IN_MS` | `5000` | Intervalle éviction (5s en dev) |
| `MANAGEMENT_SERVER_PORT` | `9090` | Port Actuator |
| `SERVER_PORT` | `8761` | Port Eureka |

#### Sourcer les variables
```bash
source .env.dev
echo $EUREKA_USER  # Vérifier
```

---

## 🗺️ Architecture Eureka

### Rôle d'Eureka Server

Eureka est le **service de découverte** centralisé :

```
EUREKA SERVER (Port 8761)
    ↓
Registre de tous les microservices :
├─ api-gateway
├─ config-server
├─ auth-service
├─ user-service
├─ order-service
├─ payment-service
├─ delivery-service
├─ notification-service
├─ location-service
├─ analytics-service
├─ promotion-service
├─ review-service
└─ support-service
```

### Endpoints Eureka

```bash
# Dashboard web
http://localhost:8761/

# Applications enregistrées
curl -u eureka:eureka123 http://localhost:8761/eureka/apps

# Informations d'une application
curl -u eureka:eureka123 http://localhost:8761/eureka/apps/api-gateway

# Enregistrer un service
curl -X POST http://localhost:8761/eureka/apps/SERVICE-NAME \
  -H "Content-Type: application/json" \
  -d '{...}'
```

---

## 📋 Enregistrement des Services

### Bootstrap pour les clients Eureka

Chaque microservice doit avoir dans son `bootstrap.yml` :

```yaml
spring:
  application:
    name: api-gateway

eureka:
  client:
    service-url:
      defaultZone: http://eureka:eureka123@eureka:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.value}
```

### Vérifier les enregistrements

```bash
# Lister tous les services enregistrés
curl -u eureka:eureka123 http://localhost:8761/eureka/apps | jq

# Afficher les instances d'un service
curl -u eureka:eureka123 http://localhost:8761/eureka/apps/api-gateway | jq
```

---

## 🌐 Dashboard Eureka

### Accéder au dashboard

```
http://localhost:8761/
```

Credentials :
- Username : `eureka`
- Password : `eureka123`

### Informations affichées

- **Applications** : Liste des services enregistrés
- **Instances** : Instances de chaque service (UP/DOWN)
- **Status** : État de santé générale
- **System Status** : Propriétés du serveur Eureka

---

## ✅ Vérification de la Santé

### Health Check (Port 9090)
```bash
curl http://localhost:9090/actuator/health
# Réponse : {"status":"UP"}
```

### Vérifier Eureka est accessible
```bash
curl -u eureka:eureka123 http://localhost:8761/eureka/apps
```

### Afficher les métriques
```bash
curl http://localhost:9090/actuator/metrics | jq
```

### Afficher les informations
```bash
curl http://localhost:9090/actuator/info | jq
```

---

## 🐳 Docker Development

### Builder l'image dev
```bash
docker build -f Dockerfile.dev -t speedline/eureka-server:dev .
```

### Lancer le conteneur avec network
```bash
docker network create speedline-network 2>/dev/null || true

docker run -p 8761:8761 -p 9090:9090 \
  --network speedline-network \
  --env-file .env.dev \
  --name eureka-server-dev \
  speedline/eureka-server:dev
```

### Afficher les logs
```bash
docker logs -f eureka-server-dev
```

---

## 🔧 Troubleshooting

### Problème : Port 8761 déjà utilisé
```bash
# Trouver le processus
lsof -i :8761

# Tuer le processus
kill -9 <PID>

# Ou utiliser un autre port
export SERVER_PORT=8762
```

### Problème : Authentification échoue
```bash
# Vérifier les credentials
echo "EUREKA_USER=$EUREKA_USER"
echo "EUREKA_PASSWORD=$EUREKA_PASSWORD"

# Les identifiants par défaut en dev :
curl -u eureka:eureka123 http://localhost:8761/eureka/apps
```

### Problème : Services ne s'enregistrent pas
```bash
# Vérifier que Eureka est accessible
curl -u eureka:eureka123 http://localhost:8761/eureka/apps

# Vérifier les logs du microservice
docker logs <service-name> | grep -i "eureka\|register"

# Vérifier la configuration eureka.client.service-url
```

### Problème : Self-preservation activée (Staging/Prod)
```bash
# En dev, self-preservation est désactivée pour test rapides
# En staging/prod, elle est activée pour stabilité

# Les services restent registrés même non accessible (5+ minutes)
# C'est normal en production
```

---

## 🔔 Note Pub/Sub & GCP

Eureka Server n'utilise pas Google Cloud Pub/Sub — c'est un service de découverte (registry) et il n'y a pas besoin d'un bus d'événements pour son fonctionnement.

Si tu veux centraliser des variables GCP (par exemple `GCP_PROJECT_ID`) afin que tous les microservices (y compris Eureka clients) les récupèrent automatiquement, fais-le via le `config-server` :

1. Ajouter une propriété dans `config-server` (ex: `src/main/resources/config/common.yml` ou directement dans `src/main/resources/config/*.yml` pour chaque profil) :

```yaml
spring:
  cloud:
    gcp:
      project-id: ${GCP_PROJECT_ID:your-default-project}
```

2. Dans les services clients (ex: `auth-service`, `api-gateway`), ne pas hardcoder `GCP_PROJECT_ID` — ils recevront la valeur depuis le Config Server au démarrage.

3. En local tu peux continuer à utiliser `.env.dev` pour surcharger `GCP_PROJECT_ID` (utile pour l'émulateur Pub/Sub).

En résumé : pas de modification nécessaire pour `eureka-server` concernant Pub/Sub — la recommandation est de propager les variables GCP via le `config-server` si tu veux centraliser la configuration.

---

## 📝 Checklist Démarrage

- [ ] Java 17+ installé
- [ ] Maven 3.8+ installé
- [ ] Fichier `.env.dev` existe
- [ ] Compiler : `mvn clean package`
- [ ] Démarrer Eureka
- [ ] Vérifier santé : `curl http://localhost:9090/actuator/health`
- [ ] Accéder au dashboard : `http://localhost:8761/`
- [ ] Vérifier authentification : `curl -u eureka:eureka123 http://localhost:8761/eureka/apps`

---

## 📚 Ressources

- **Spring Cloud Eureka** : https://spring.io/projects/spring-cloud-eureka
- **Eureka Documentation** : https://github.com/Netflix/eureka/wiki
- **Spring Boot Actuator** : https://spring.io/guides/gs/actuator-service/

---

**Dernière mise à jour** : 2026-02-23  
**Auteur** : GitHub Copilot  
**Version** : 1.0.0
