# 📚 GUIDE STAGING - ANALYTICS SERVICE

## ▶️ Démarrage

```bash
source .env.staging
docker-compose -f docker-compose.staging.yml up analytics-service
```

## 🔐 Base de Données Staging

```bash
# Créer la base
mysql -u analytics_user -p
CREATE DATABASE analytics_staging CHARACTER SET utf8mb4;
```

## ✅ Vérification

```bash
# Health
curl http://localhost:9090/actuator/health

# Eureka
curl -u eureka:eureka123 http://eureka:8761/eureka/apps/analytics-service
```

---

**Profil** : Staging

