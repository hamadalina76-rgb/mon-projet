# 🚀 Guide de Démarrage - SpeedLine Backend

Guide complet pour démarrer tous les services SpeedLine en local (pour les stagiaires).

## 📋 Prérequis

Avant de commencer, assurez-vous d'avoir installé :

- ✅ **Java 17+** (vérifier avec `java -version`)
- ✅ **Maven 3.8+** (vérifier avec `mvn -version`)
- ✅ **Docker Desktop** (Windows/Mac) ou Docker (Linux)
- ✅ **Git** (pour cloner le projet)

## 🎯 Étape 1 : Préparation de l'environnement

### 1.1 Cloner le projet (si pas déjà fait)

```bash
git clone <url-du-repo>
cd SpeedLine/speedline/backend
```

### 1.2 Vérifier le fichier `.env`

Assurez-vous que le fichier `.env` existe à la racine du dossier `backend` avec les bonnes valeurs :

```env
DB_PASSWORD=postgres123
MONGO_PASSWORD=mongo123
REDIS_PASSWORD=your_redis_password_here
CLICKHOUSE_PASSWORD=your_clickhouse_password_here
JWT_SECRET=your_jwt_secret_key_here_at_least_256_bits_long
```

## 🐳 Étape 2 : Démarrer les services d'infrastructure (Docker)

**IMPORTANT :** Ces services doivent être démarrés EN PREMIER avant tous les microservices Spring Boot.

### 2.1 Démarrer Docker Desktop

- Ouvrez Docker Desktop
- Attendez qu'il soit complètement démarré (icône Docker dans la barre des tâches)

### 2.2 Démarrer les conteneurs Docker

```powershell
# Se placer dans le dossier backend
cd c:\Users\user\Desktop\SpeedLine\speedline\backend

# Démarrer tous les services d'infrastructure
docker-compose up -d postgres mongodb redis clickhouse kafka zookeeper kafka-ui eureka-server
```

### 2.3 Vérifier que les services sont démarrés

```powershell
docker-compose ps
```

Vous devriez voir tous les services avec le statut "Up" :
- ✅ postgres
- ✅ mongodb
- ✅ redis
- ✅ clickhouse
- ✅ kafka
- ✅ zookeeper
- ✅ kafka-ui
- ✅ eureka-server

### 2.4 Vérifier les bases de données PostgreSQL

```powershell
docker exec postgres psql -U postgres -c "\l" | Select-String "speedline"
```

Vous devriez voir toutes les bases de données SpeedLine créées automatiquement.

## 🔄 Étape 3 : Ordre de démarrage des microservices

**ATTENTION :** Respectez cet ordre pour éviter les erreurs de dépendances !

### Ordre de démarrage recommandé :

```
1. Eureka Server (déjà démarré via Docker)
   ↓
2. Config Server (si utilisé)
   ↓
3. Auth Service
   ↓
4. User Service
   ↓
5. Partner Service
   ↓
6. Location Service
   ↓
7. Payment Service
   ↓
8. Promotion Service
   ↓
9. Order Service
   ↓
10. Delivery Service
    ↓
11. Notification Service
    ↓
12. Review Service
    ↓
13. Support Service
    ↓
14. Analytics Service
    ↓
15. API Gateway (en dernier)
```

## 🚀 Étape 4 : Démarrer les microservices

### Option A : Depuis votre IDE (IntelliJ IDEA / Eclipse)

1. Ouvrez le projet dans votre IDE
2. Pour chaque service, trouvez la classe `*Application.java` (ex: `AuthServiceApplication.java`)
3. Clic droit → Run 'AuthServiceApplication'
4. Répétez pour chaque service dans l'ordre ci-dessus

### Option B : Depuis le terminal avec Maven

```powershell
# 1. Auth Service
cd services\auth-service
mvn spring-boot:run

# Dans un NOUVEAU terminal :
# 2. User Service
cd services\user-service
mvn spring-boot:run

# Dans un NOUVEAU terminal :
# 3. Partner Service
cd services\partner-service
mvn spring-boot:run

# ... et ainsi de suite pour chaque service
```

### Option C : Script PowerShell (recommandé pour les stagiaires)

Créez un fichier `start-all-services.ps1` à la racine du dossier `backend` :

```powershell
# Script de démarrage de tous les services
Write-Host "Démarrage des services SpeedLine..." -ForegroundColor Cyan

# Fonction pour démarrer un service
function Start-Service {
    param($ServiceName, $Port)
    Write-Host "Démarrage de $ServiceName sur le port $Port..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd services\$ServiceName; mvn spring-boot:run"
    Start-Sleep -Seconds 10
}

# Démarrer les services dans l'ordre
Start-Service "auth-service" "8081"
Start-Service "user-service" "8082"
Start-Service "partner-service" "8083"
Start-Service "location-service" "8088"
Start-Service "payment-service" "8086"
Start-Service "promotion-service" "8092"
Start-Service "order-service" "8084"
Start-Service "delivery-service" "8085"
Start-Service "notification-service" "8087"
Start-Service "review-service" "8091"
Start-Service "support-service" "8093"
Start-Service "analytics-service" "8089"
Start-Service "api-gateway" "8080"

Write-Host "Tous les services sont en cours de démarrage..." -ForegroundColor Green
Write-Host "Vérifiez les logs dans chaque fenêtre PowerShell" -ForegroundColor Green
```

## ✅ Étape 5 : Vérifier que tout fonctionne

### 5.1 Vérifier Eureka (Service Discovery)

Ouvrez votre navigateur et allez sur : **http://localhost:8761**

Vous devriez voir tous les services enregistrés avec le statut "UP".

### 5.2 Vérifier les logs

Dans chaque terminal/fenêtre de service, vous devriez voir :
- ✅ `Started [ServiceName]Application in X.XXX seconds`
- ✅ `Registered with Eureka`
- ✅ Pas d'erreurs de connexion à la base de données

### 5.3 Vérifier les ports

```powershell
netstat -ano | findstr "LISTENING" | findstr ":808"
```

Vous devriez voir tous les ports des services actifs.

## 📊 Ports des Services

| Service | Port | Base de données |
|---------|------|-----------------|
| Eureka Server | 8761 | - |
| Config Server | 8888 | - |
| API Gateway | 8080 | - |
| Auth Service | 8081 | PostgreSQL (speedline_auth) |
| User Service | 8082 | PostgreSQL (speedline_users) |
| Partner Service | 8083 | PostgreSQL (speedline_partners) |
| Order Service | 8084 | PostgreSQL (speedline_orders) |
| Delivery Service | 8085 | PostgreSQL (speedline_delivery) |
| Payment Service | 8086 | PostgreSQL (speedline_payments) |
| Notification Service | 8087 | MongoDB (speedline_notifications) |
| Location Service | 8088 | PostgreSQL (speedline_locations) |
| Analytics Service | 8089 | ClickHouse |
| Review Service | 8091 | MongoDB (speedline_reviews) |
| Promotion Service | 8092 | PostgreSQL (speedline_promotions) |
| Support Service | 8093 | PostgreSQL (speedline_support) |
| Kafka UI | 8090 | - |

## 🛠️ Services d'Infrastructure

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5432 | Base de données principale |
| MongoDB | 27017 | Base de données NoSQL |
| Redis | 6379 | Cache et sessions |
| ClickHouse | 8123, 9000 | Analytics et données volumineuses |
| Kafka | 9092 | Messagerie asynchrone |
| Zookeeper | 2181 | Coordination pour Kafka |

## ⚠️ Problèmes Courants et Solutions

### Problème 1 : "Port already in use"

**Solution :**
```powershell
# Trouver le processus qui utilise le port
netstat -ano | findstr :8081

# Arrêter le processus (remplacer PID par le numéro trouvé)
taskkill /F /PID <PID>
```

### Problème 2 : "Connection to localhost:5432 refused"

**Solution :**
1. Vérifiez que Docker est démarré : `docker ps`
2. Vérifiez que PostgreSQL est démarré : `docker-compose ps postgres`
3. Redémarrez PostgreSQL : `docker-compose restart postgres`

### Problème 3 : "Migration checksum mismatch"

**Solution :**
```powershell
# Se connecter à la base de données concernée
docker exec -it postgres psql -U postgres -d speedline_<service>

# Supprimer l'entrée problématique
DELETE FROM flyway_schema_history WHERE version = '<version>';
```

### Problème 4 : "Missing column [column_name]"

**Solution :**
- Les migrations Flyway doivent être appliquées automatiquement
- Si le problème persiste, vérifiez que la migration V2 existe dans `src/main/resources/db/migration/`
- Redémarrez le service

### Problème 5 : Service ne s'enregistre pas dans Eureka

**Solution :**
1. Vérifiez que Eureka Server est démarré : http://localhost:8761
2. Vérifiez les logs du service pour les erreurs de connexion
3. Vérifiez les credentials Eureka dans `application.yml`

## 🛑 Arrêter tous les services

### Arrêter les microservices Spring Boot

- Fermez toutes les fenêtres PowerShell/terminaux où les services tournent
- Ou utilisez `Ctrl+C` dans chaque terminal

### Arrêter les services Docker

```powershell
# Arrêter tous les services
docker-compose down

# Arrêter et supprimer les volumes (⚠️ supprime les données)
docker-compose down -v
```

## 📝 Checklist de Démarrage Rapide

- [ ] Docker Desktop est démarré
- [ ] Les services d'infrastructure sont démarrés (`docker-compose ps`)
- [ ] Les bases de données PostgreSQL existent
- [ ] Eureka Server est accessible sur http://localhost:8761
- [ ] Auth Service est démarré (port 8081)
- [ ] User Service est démarré (port 8082)
- [ ] Partner Service est démarré (port 8083)
- [ ] Les autres services sont démarrés dans l'ordre
- [ ] Tous les services apparaissent dans Eureka avec le statut "UP"
- [ ] Aucune erreur dans les logs

## 🎓 Conseils pour les Stagiaires

1. **Commencez par un seul service** : Ne démarrez pas tout d'un coup. Commencez par Auth Service, puis ajoutez les autres un par un.

2. **Surveillez les logs** : Les logs vous indiquent ce qui ne va pas. Lisez-les attentivement.

3. **Vérifiez Eureka** : C'est votre tableau de bord. Si un service n'apparaît pas, il y a un problème.

4. **Utilisez les ports par défaut** : Ne changez pas les ports sauf si nécessaire (conflit).

5. **En cas d'erreur** : 
   - Lisez le message d'erreur complet
   - Vérifiez la section "Problèmes Courants" ci-dessus
   - Demandez de l'aide si nécessaire

6. **Sauvegardez votre travail** : Les migrations de base de données sont importantes. Ne les modifiez pas sans comprendre l'impact.

## 📚 Ressources Utiles

- **Eureka Dashboard** : http://localhost:8761
- **Kafka UI** : http://localhost:8090
- **Documentation Spring Boot** : https://spring.io/projects/spring-boot
- **Documentation Flyway** : https://flywaydb.org/documentation/

---

**Bon développement ! 🚀**
