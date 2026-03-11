# 📚 GUIDE PRODUCTION - ANALYTICS SERVICE

## ▶️ Déploiement Kubernetes

```bash
source .env.prod
kubectl apply -f k8s/analytics-service-prod.yml
```

## 🔐 Base de Données Production

Variables depuis Secrets Manager :
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## ✅ Vérification

```bash
# Health (interne)
kubectl exec analytics-service-0 -- curl http://localhost:9090/actuator/health

# Eureka
curl -u ${EUREKA_USER_PROD}:${EUREKA_PASSWORD_PROD} http://eureka:8761/eureka/apps/analytics-service
```

---

**Profil** : Production (CRITIQUE)

