# 🚀 Guide de Démarrage Rapide - SpeedLine

## 📋 Prérequis

### Obligatoires
- ✅ **Docker Desktop** (version 20.10+)
- ✅ **Docker Compose** (version 2.0+)
- ✅ **Java 17+** (pour développement local sans Docker)
- ✅ **Maven 3.8+** (pour compilation)

### Recommandés
- IntelliJ IDEA / Eclipse / VS Code
- Postman (pour tester les API)
- Git

---

## 🔧 Configuration Initiale

### 1. Créer le fichier d'environnement

```bash
cd backend
cp .env.example .env
```

### 2. Modifier `.env` avec vos vraies valeurs

```bash
# Ouvrir le fichier .env et modifier:

DB_PASSWORD=postgres123
MONGO_PASSWORD=mongo123
REDIS_PASSWORD=redis123
CLICKHOUSE_PASSWORD=clickhouse123
JWT_SECRET=monSecretJWTSuperLongDauMoins256BitsIciPourSpeedLine2026
```

**⚠️ Important:** Les autres services externes (Stripe, Twilio, etc.) ne sont pas obligatoires pour tester localement.

---

## 🐳 Option 1: Démarrage avec Docker (Recommandé)

### Étape 1: Démarrer uniquement l'infrastructure

```bash
# Depuis le dossier backend/
docker-compose up -d postgres mongodb redis kafka zookeeper eureka-server
```

**Attendez 30 secondes** que tout démarre correctement.

### Étape 2: Vérifier que l'infrastructure est OK

```bash
# Vérifier les conteneurs
docker-compose ps

# Vérifier Eureka (Service Discovery)
curl http://localhost:8761
```

Ouvrez http://localhost:8761 dans votre navigateur - vous devriez voir le dashboard Eureka.

### Étape 3: Démarrer Config Server & API Gateway

```bash
docker-compose up -d config-server api-gateway
```

### Étape 4: Démarrer un service de test (Auth Service)

```bash
docker-compose up -d auth-service
```

### Étape 5: Tester l'API

```bash
# Test santé Auth Service
curl http://localhost:8081/actuator/health

# Test via Gateway
curl http://localhost:8080/api/auth/health
```

### Démarrer TOUS les services

```bash
# Démarrer tout d'un coup
docker-compose up -d

# Voir les logs
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f auth-service
```

---

## 💻 Option 2: Démarrage Local (Sans Docker pour les services)

### Étape 1: Démarrer uniquement les bases de données avec Docker

```bash
docker-compose up -d postgres mongodb redis kafka zookeeper clickhouse
```

### Étape 2: Compiler tous les services

```bash
# Depuis le dossier backend/
mvn clean install -DskipTests
```

### Étape 3: Démarrer les services dans l'ordre

**Terminal 1 - Eureka:**
```bash
cd eureka-server
mvn spring-boot:run
```

**Terminal 2 - Config Server (attendre que Eureka soit UP):**
```bash
cd config-server
mvn spring-boot:run
```

**Terminal 3 - API Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```

**Terminal 4 - Auth Service:**
```bash
cd services/auth-service
mvn spring-boot:run
```

**Terminal 5 - User Service:**
```bash
cd services/user-service
mvn spring-boot:run
```

... et ainsi de suite pour les autres services.

---

## 🧪 Vérification du Système

### 1. Vérifier Eureka Dashboard

Ouvrez http://localhost:8761

Vous devriez voir tous les services enregistrés:
- API-GATEWAY
- AUTH-SERVICE
- USER-SERVICE
- PARTNER-SERVICE
- etc.

### 2. Vérifier Kafka UI

Ouvrez http://localhost:8090

Vous verrez les topics Kafka créés.

### 3. Tester une requête complète

```bash
# 1. Créer un utilisateur
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@speedline.com",
    "password": "Test123!",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+21612345678"
  }'

# 2. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@speedline.com",
    "password": "Test123!"
  }'
```

---

## 📊 Ports des Services

| Service | Port | URL |
|---------|------|-----|
| **Eureka Dashboard** | 8761 | http://localhost:8761 |
| **API Gateway** | 8080 | http://localhost:8080 |
| **Kafka UI** | 8090 | http://localhost:8090 |
| Auth Service | 8081 | http://localhost:8081 |
| User Service | 8082 | http://localhost:8082 |
| Partner Service | 8083 | http://localhost:8083 |
| Order Service | 8084 | http://localhost:8084 |
| Delivery Service | 8085 | http://localhost:8085 |
| Payment Service | 8086 | http://localhost:8086 |
| Notification Service | 8087 | http://localhost:8087 |
| Location Service | 8088 | http://localhost:8088 |
| Analytics Service | 8089 | http://localhost:8089 |
| Promotion Service | 8090 | http://localhost:8090 |
| Review Service | 8091 | http://localhost:8091 |
| Support Service | 8092 | http://localhost:8092 |

---

## 🛠️ Commandes Utiles

### Docker

```bash
# Voir tous les conteneurs
docker-compose ps

# Voir les logs
docker-compose logs -f [service-name]

# Redémarrer un service
docker-compose restart [service-name]

# Arrêter tout
docker-compose down

# Arrêter et supprimer les volumes (⚠️ Perte de données!)
docker-compose down -v

# Rebuild un service après modification
docker-compose up -d --build [service-name]
```

### Maven

```bash
# Compiler sans tests
mvn clean install -DskipTests

# Compiler avec tests
mvn clean install

# Démarrer un service
mvn spring-boot:run

# Compiler un service spécifique
cd services/auth-service && mvn clean install
```

---

## ❌ Résolution de Problèmes

### Problème: "Port already in use"

```bash
# Trouver le processus qui utilise le port 8080
lsof -i :8080  # Mac/Linux
netstat -ano | findstr :8080  # Windows

# Tuer le processus
kill -9 [PID]  # Mac/Linux
taskkill /PID [PID] /F  # Windows
```

### Problème: Service ne démarre pas

```bash
# Voir les logs détaillés
docker-compose logs [service-name]

# Vérifier la santé
docker-compose ps
```

### Problème: Eureka ne voit pas les services

1. Vérifier que Eureka est bien démarré: http://localhost:8761
2. Attendre 30-60 secondes (enregistrement prend du temps)
3. Vérifier les logs du service

### Problème: Base de données vide

```bash
# Recréer les bases
docker-compose down -v
docker-compose up -d postgres

# Vérifier les logs
docker-compose logs postgres
```

---

## 🔄 Workflow de Développement Recommandé

### Pour développer un service:

1. **Démarrer l'infrastructure:**
   ```bash
   docker-compose up -d postgres mongodb redis kafka eureka-server config-server api-gateway
   ```

2. **Développer en local (IDE):**
   ```bash
   cd services/auth-service
   mvn spring-boot:run
   ```

3. **Tester:**
   - Utiliser Postman
   - Vérifier Eureka Dashboard
   - Consulter les logs

4. **Rebuild si nécessaire:**
   ```bash
   mvn clean install
   ```

---

## 📦 Ordre de Démarrage Recommandé

1. **Infrastructure (toujours en premier):**
   - PostgreSQL
   - MongoDB
   - Redis
   - Kafka + Zookeeper

2. **Services Core:**
   - Eureka Server (8761)
   - Config Server (8888)
   - API Gateway (8080)

3. **Services Métier (ordre flexible):**
   - Auth Service (8081) ← Commencer par celui-ci
   - User Service (8082)
   - Partner Service (8083)
   - Order Service (8084)
   - Delivery Service (8085)
   - Payment Service (8086)
   - Notification Service (8087)
   - Location Service (8088)
   - Analytics Service (8089)
   - Promotion Service (8090)
   - Review Service (8091)
   - Support Service (8092)

---

## 🎯 Quick Start pour les Stagiaires

**Pour commencer rapidement sans tout configurer:**

```bash
# 1. Copier le fichier d'environnement
cp .env.example .env

# 2. Modifier juste les mots de passe de base
nano .env  # ou notepad .env sur Windows

# 3. Démarrer UNIQUEMENT l'infrastructure + Auth
docker-compose up -d postgres redis kafka eureka-server auth-service

# 4. Attendre 60 secondes

# 5. Tester
curl http://localhost:8761  # Eureka
curl http://localhost:8081/actuator/health  # Auth Service

# 6. Développer votre service
cd services/user-service
mvn spring-boot:run
```

---

## 📚 Ressources Utiles

- **Eureka Dashboard:** http://localhost:8761
- **Kafka UI:** http://localhost:8090
- **API Gateway:** http://localhost:8080
- **README Principal:** [README.md](README.md)

---

**Bon développement ! 🚀**
