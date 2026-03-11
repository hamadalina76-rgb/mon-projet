# 📚 GUIDE STAGING - EUREKA SERVER SPEEDLINE

## ▶️ Démarrage avec Docker

```bash
source .env.staging

docker build -f Dockerfile.staging -t speedline/eureka-server:staging .

docker run -p 8761:8761 -p 9090:9090 \
  --network speedline-network \
  --env-file .env.staging \
  speedline/eureka-server:staging
```

## 🔐 Variables Staging

| Variable | Valeur |
|----------|--------|
| `EUREKA_USER` | `eureka-staging` |
| `EUREKA_PASSWORD` | `eureka-staging-*` |
| `EUREKA_INSTANCE_HOSTNAME` | `eureka` |
| `EUREKA_SERVER_ENABLE_SELF_PRESERVATION` | `true` |
| `EUREKA_SERVER_EVICTION_INTERVAL_TIMER_IN_MS` | `10000` |

## ✅ Vérification

```bash
# Health check
curl http://localhost:9090/actuator/health

# Applications enregistrées
curl -u eureka-staging:password http://localhost:8761/eureka/apps

# Dashboard
http://localhost:8761/
```

---

**Profil** : Staging (Pré-production)

