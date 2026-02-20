# Guide de Développement Local - SpeedLine

Ce guide explique comment démarrer les services SpeedLine en local pour le développement.

## Prérequis

- Java 17+
- Maven 3.8+
- Docker et Docker Compose (recommandé)
- PostgreSQL (si vous ne voulez pas utiliser Docker)

## Option 1 : Utiliser Docker Compose (Recommandé)

La méthode la plus simple est d'utiliser Docker Compose pour démarrer toutes les dépendances :

### 1. Démarrer les services d'infrastructure

```bash
cd speedline/backend
docker-compose up -d postgres mongodb redis clickhouse pubsub-emulator eureka-server
```

Cela démarre :
- **PostgreSQL** sur le port 5432
- **MongoDB** sur le port 27017
- **Redis** sur le port 6379
- **ClickHouse** sur les ports 8123 et 9000
- **Pub/Sub Emulator** sur le port 8085
- **Eureka Server** sur le port 8761

### 2. Vérifier que les services sont démarrés

```bash
docker-compose ps
```

### 3. Lancer les services Spring Boot

Vous pouvez maintenant lancer les services individuellement depuis votre IDE ou avec Maven :

```bash
# Auth Service
cd services/auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Analytics Service
cd services/analytics-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Option 2 : Développement sans Docker

Si vous préférez ne pas utiliser Docker, vous pouvez :

### 1. Installer PostgreSQL localement

- Téléchargez et installez PostgreSQL depuis https://www.postgresql.org/download/
- Créez les bases de données nécessaires :
  ```sql
  CREATE DATABASE speedline_auth;
  CREATE DATABASE speedline_users;
  CREATE DATABASE speedline_partners;
  CREATE DATABASE speedline_orders;
  CREATE DATABASE speedline_delivery;
  CREATE DATABASE speedline_payments;
  CREATE DATABASE speedline_locations;
  CREATE DATABASE speedline_promotions;
  CREATE DATABASE speedline_support;
  ```

### 2. Installer MongoDB localement

- Téléchargez MongoDB depuis https://www.mongodb.com/try/download/community
- Démarrez MongoDB

### 3. Installer Redis localement

- Windows : Utilisez WSL ou Docker
- Linux/Mac : `brew install redis` ou `apt-get install redis`

### 4. Lancer les services avec le profil local

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Configuration des Variables d'Environnement

Créez un fichier `.env` à la racine du projet backend avec :

```env
DB_PASSWORD=postgres
MONGO_PASSWORD=mongo123
REDIS_PASSWORD=redis123
CLICKHOUSE_PASSWORD=clickhouse123
JWT_SECRET=votre-secret-jwt-super-long-et-securise-minimum-256-bits
```

## Profils Spring Boot Disponibles

- **local** : Pour le développement local sans Docker
- **docker** : Pour l'exécution dans Docker Compose
- **default** : Configuration par défaut

## Résolution des Problèmes Courants

### Erreur : "Connection to localhost:5432 refused"

**Solution** : 
1. Vérifiez que PostgreSQL est démarré : `docker-compose ps` ou `pg_isready`
2. Utilisez le profil `local` : `-Dspring-boot.run.profiles=local`
3. Vérifiez les variables d'environnement `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`

### Erreur : "Failed to configure a DataSource"

**Solution** :
- Pour `analytics-service` : Ce service utilise ClickHouse, pas PostgreSQL. L'auto-configuration DataSource est désactivée.
- Pour les autres services : Assurez-vous que PostgreSQL est démarré et accessible.

### Erreur : "Eureka connection refused"

**Solution** :
- Démarrez Eureka Server : `docker-compose up -d eureka-server`
- Ou désactivez Eureka avec `EUREKA_ENABLED=false` dans le profil local

## Commandes Utiles

```bash
# Voir les logs d'un service
docker-compose logs -f auth-service

# Redémarrer un service
docker-compose restart postgres

# Arrêter tous les services
docker-compose down

# Arrêter et supprimer les volumes (⚠️ supprime les données)
docker-compose down -v
```

## Ports Utilisés

| Service | Port |
|---------|------|
| Eureka Server | 8761 |
| Config Server | 8888 |
| API Gateway | 8080 |
| Auth Service | 8081 |
| User Service | 8082 |
| Partner Service | 8083 |
| Order Service | 8084 |
| Delivery Service | 8085 |
| Payment Service | 8086 |
| Notification Service | 8087 |
| Location Service | 8088 |
| Analytics Service | 8089 |
| Promotion Service | 8092 |
| Review Service | 8091 |
| Support Service | 8093 |
| PostgreSQL | 5432 |
| MongoDB | 27017 |
| Redis | 6379 |
| ClickHouse HTTP | 8123 |
| ClickHouse Native | 9000 |
| Pub/Sub Emulator | 8085 |
