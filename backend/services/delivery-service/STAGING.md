# 📚 GUIDE STAGING - DELIVERY SERVICE

## ▶️ Démarrage avec Docker

```bash
source .env.staging

# Builder l'image
docker build -f Dockerfile.staging -t speedline/delivery-service:staging .

# Lancer le conteneur
docker run -p 8085:8085 -p 9090:9090 \
  --network speedline-network \
  --env-file .env.staging \
  speedline/delivery-service:staging

# Logs
docker logs -f delivery-service-staging
```

---

## 🔐 Base de Données Staging

```bash
# Créer la base
psql -U postgres
CREATE DATABASE speedline_delivery_staging;

# Vérifier les migrations
SELECT * FROM flyway_schema_history;
```

---

## 📨 Kafka Staging

```bash
# Vérifier les topics
kafka-topics --bootstrap-server kafka:9092 --list

# Consommer des messages
kafka-console-consumer --bootstrap-server kafka:9092 \
  --topic delivery.created --from-beginning
```

---

## ✅ Vérification

```bash
# Health
curl http://localhost:9090/actuator/health

# Eureka
curl -u eureka:eureka123 http://eureka:8761/eureka/apps/delivery-service

# Metrics
curl http://localhost:9090/actuator/metrics
```

---

## 🌐 Docker Compose Staging

```bash
docker-compose -f docker-compose.staging.yml up -d delivery-service
docker-compose -f docker-compose.staging.yml logs -f delivery-service
```

---

**Profil** : Staging (Pré-production)  
**Port** : 8085

