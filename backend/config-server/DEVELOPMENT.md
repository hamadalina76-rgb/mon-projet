# 📚 GUIDE DÉVELOPPEMENT - CONFIG SERVER SPEEDLINE

## 📋 Table des matières

1. [Configuration Initiale](#configuration-initiale)
2. [Démarrage du Service](#démarrage-du-service)
3. [Variables d'Environnement](#variables-denvironnement)
4. [Architecture Config Server](#architecture-config-server)
5. [Gestion des Configurations](#gestion-des-configurations)
6. [Vérification de la Santé](#vérification-de-la-santé)
7. [Docker Development](#docker-development)
8. [Troubleshooting](#troubleshooting)

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

### Cloner et compiler le projet
```bash
cd /home/nayer/IdeaProjects/speedline/backend/config-server
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
   -Dspring.profiles.active=dev
4. Ajouter Environment variables depuis .env.dev
5. Run → Run 'ConfigServerApplication'
```

#### Option B : Ligne de commande
```bash
# Charger les variables d'environnement
source .env.dev

# Compiler le projet
mvn clean package -DskipTests

# Démarrer l'application
SPRING_PROFILES_ACTIVE=dev java -jar target/config-server-1.0.0.jar
```

### 2️⃣ Démarrage avec Docker

```bash
# Charger les variables
source .env.dev

# Builder l'image dev
docker build -f Dockerfile.dev -t speedline/config-server:dev .

# Lancer le conteneur
docker run -p 8888:8888 -p 9090:9090 \
  --env-file .env.dev \
  --name config-server-dev \
  speedline/config-server:dev

# Afficher les logs
docker logs -f config-server-dev
```

### 3️⃣ Démarrage avec Docker Compose

```bash
# À la racine du backend
docker-compose up config-server

# Avec logs
docker-compose up -d config-server && docker-compose logs -f config-server
```

---

## 🔐 Variables d'Environnement

### Fichier `.env.dev`

Ce fichier contient toutes les variables d'environnement pour le développement.

#### Variables Principales

| Variable | Valeur Par Défaut | Description |
|----------|------------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Profil Spring actif |
| `CONFIG_USER` | `config` | Utilisateur pour accéder au config server |
| `CONFIG_PASSWORD` | `config123` | Mot de passe du config server |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | `http://eureka:eureka123@localhost:8761/eureka/` | URL Eureka Server |
| `CONFIG_SERVER_SEARCH_LOCATIONS` | `classpath:/config` | Localisation des configurations |
| `MANAGEMENT_SERVER_PORT` | `9090` | Port Actuator |
| `SERVER_PORT` | `8888` | Port du config server |

#### Sourcer les variables (Linux/Mac)
```bash
source .env.dev
echo $CONFIG_USER  # Vérifier
```

---

## ⚙️ Fichiers de Configuration

### Structure
```
config-server/
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
    ├── application-prod.yml          # Config production
    └── config/                       # Configurations applicatives
        ├── application.yml           # Config commune
        ├── application-dev.yml       # Config dev
        └── ...
```

### application-dev.yml (Configuration développement)
✅ **Contenu** :
- Logs DEBUG activés
- Health checks complets
- Config server en mode native (classpath:/config)
- Credentials simples pour dev
- Affichage des propriétés (configprops)

---

## 🗺️ Architecture Config Server

### Rôle du Config Server

Le Config Server est le **centre centralisé** de configuration pour tous les microservices :

```
CONFIG SERVER (Port 8888)
    ↓
Fournit les configurations pour :
├─ api-gateway
├─ eureka-server
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

### Endpoints Config Server

```bash
# Récupérer la configuration d'un service
curl -u config:config123 http://localhost:8888/api-gateway/dev

# Récupérer la configuration brute
curl -u config:config123 http://localhost:8888/api-gateway-dev.yml

# Health check
curl http://localhost:9090/actuator/health

# Lister les propriétés
curl -u config:config123 http://localhost:9090/actuator/configprops
```

---

## 📋 Gestion des Configurations

### Ajouter une nouvelle configuration

#### 1. Créer le fichier dans `src/main/resources/config/`

```bash
# Pour api-gateway
touch src/main/resources/config/api-gateway-dev.yml

# Pour auth-service
touch src/main/resources/config/auth-service-dev.yml
```

#### 2. Ajouter les propriétés

```yaml
# api-gateway-dev.yml
spring:
  redis:
    host: localhost
    port: 6379

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway,metrics
```

#### 3. Recharger la configuration

```bash
# Les microservices rechargent automatiquement la config
# Ou forcer le rechargement avec :
curl -X POST -u config:config123 http://localhost:8080/actuator/refresh
```

---

## ✅ Vérification de la Santé

### Health Check (Port 9090)
```bash
curl http://localhost:9090/actuator/health
# Réponse : {"status":"UP"}
```

### Afficher les configurations chargées
```bash
curl -u config:config123 http://localhost:9090/actuator/configprops | jq
```

### Afficher les informations
```bash
curl http://localhost:9090/actuator/info | jq
```

---

## 🐳 Docker Development

### Builder l'image dev
```bash
docker build -f Dockerfile.dev -t speedline/config-server:dev .
```

### Lancer le conteneur
```bash
docker run -p 8888:8888 -p 9090:9090 \
  -v $(pwd)/src/main/resources/config:/config \
  --env-file .env.dev \
  speedline/config-server:dev
```

### Afficher les logs
```bash
docker logs -f config-server-dev
```

---

## 🔧 Troubleshooting

### Problème : Port 8888 déjà utilisé
```bash
# Trouver le processus
lsof -i :8888

# Tuer le processus
kill -9 <PID>

# Ou utiliser un autre port
export SERVER_PORT=8889
```

### Problème : Authentification échoue
```bash
# Vérifier les credentials
echo "CONFIG_USER=$CONFIG_USER"
echo "CONFIG_PASSWORD=$CONFIG_PASSWORD"

# Les identifiants par défaut en dev :
curl -u config:config123 http://localhost:8888/api-gateway/dev
```

### Problème : Config server ne trouvant pas les configurations
```bash
# Vérifier le chemin
ls -la src/main/resources/config/

# Vérifier que les fichiers existent
ls -la src/main/resources/config/api-gateway-dev.yml
```

### Problème : Microservices ne reçoivent pas les configs
```bash
# Vérifier que le microservice est configuré pour utiliser le config server
# Dans bootstrap.yml du microservice :
spring:
  cloud:
    config:
      uri: http://config-server:8888
      username: config
      password: config123
      profile: dev
```

---

## 📝 Checklist Démarrage

- [ ] Java 17+ installé
- [ ] Maven 3.8+ installé
- [ ] Fichier `.env.dev` existe
- [ ] Eureka Server lancé
- [ ] Fichiers de config existent dans `src/main/resources/config/`
- [ ] Compiler : `mvn clean package`
- [ ] Démarrer le config server
- [ ] Vérifier santé : `curl http://localhost:9090/actuator/health`
- [ ] Tester récupération config : `curl -u config:config123 http://localhost:8888/api-gateway/dev`

---

## 📚 Ressources

- **Spring Cloud Config** : https://spring.io/projects/spring-cloud-config
- **Spring Cloud Eureka** : https://spring.io/projects/spring-cloud-eureka
- **Spring Boot Actuator** : https://spring.io/guides/gs/actuator-service/

---

**Dernière mise à jour** : 2026-02-23  
**Auteur** : GitHub Copilot  
**Version** : 1.0.0

