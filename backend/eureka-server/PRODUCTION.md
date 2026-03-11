# 📚 GUIDE PRODUCTION - EUREKA SERVER SPEEDLINE

## ▶️ Démarrage avec Kubernetes

```bash
# Appliquer la configuration
kubectl apply -f k8s/eureka-server-prod.yml

# Vérifier le déploiement
kubectl get pods -n production
kubectl logs -f deployment/eureka-server-prod -n production

# Exposer le service
kubectl port-forward svc/eureka-server-prod 8761:8761 -n production
```

## 🔐 Secrets Management

Variables depuis AWS Secrets Manager :
```bash
EUREKA_USER=${EUREKA_USER_PROD}
EUREKA_PASSWORD=${EUREKA_PASSWORD_PROD}
```

## ⚙️ Configuration Production

| Aspect | Valeur |
|--------|--------|
| `EUREKA_SERVER_ENABLE_SELF_PRESERVATION` | `true` |
| `EUREKA_SERVER_EVICTION_INTERVAL_TIMER_IN_MS` | `30000` |
| Logs | `ERROR` |
| Actuator | `/actuator` (interne) |
| Replicas | 3+ (haute disponibilité) |

## ✅ Vérification

```bash
# Health check (interne)
kubectl exec eureka-server-0 -- curl http://localhost:9090/actuator/health

# Dashboard
http://eureka-prod:8761/

# Applications
curl -u ${EUREKA_USER_PROD}:${EUREKA_PASSWORD_PROD} \
  http://eureka-prod:8761/eureka/apps
```

---

**Profil** : Production (CRITIQUE)

