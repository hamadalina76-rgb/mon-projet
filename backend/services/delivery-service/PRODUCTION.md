# 📚 GUIDE PRODUCTION - DELIVERY SERVICE

## ▶️ Déploiement Kubernetes

```bash
# Charger les secrets
source .env.prod

# Appliquer la configuration
kubectl apply -f k8s/delivery-service-prod.yml

# Vérifier le déploiement
kubectl get pods -n production | grep delivery-service
kubectl logs -f deployment/delivery-service-prod -n production

# Exposer le service
kubectl port-forward svc/delivery-service-prod 8085:8085 -n production
```

---

## 🔐 Secrets Management

Variables depuis **AWS Secrets Manager** / **Vault** :

```bash
# Base de données
SPRING_DATASOURCE_URL=${DB_URL_PROD}
SPRING_DATASOURCE_USERNAME=${DB_USERNAME_PROD}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD_PROD}

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=${KAFKA_SERVERS_PROD}

# Eureka
EUREKA_USER=${EUREKA_USER_PROD}
EUREKA_PASSWORD=${EUREKA_PASSWORD_PROD}
```

---

## ⚙️ Configuration Production

| Aspect | Valeur |
|--------|--------|
| **JPA DDL** | `validate` (strict) |
| **Flyway baseline-on-migrate** | `false` (BD doit être propre) |
| **Logs** | `ERROR` |
| **Actuator** | `health,info` uniquement |
| **Replicas** | 3+ (haute disponibilité) |

---

## 📨 Kafka Production

### Topics Production

- `prod.delivery.created`
- `prod.delivery.assigned`
- `prod.delivery.picked-up`
- `prod.delivery.delivered`
- `prod.delivery.cancelled`

### Monitoring Kafka

```bash
# Via Kafka Manager / Confluent Control Center
# Vérifier lag des consumers
# Surveiller throughput
```

---

## ✅ Vérification

```bash
# Health check (interne)
kubectl exec delivery-service-0 -- curl http://localhost:9090/actuator/health

# Readiness probe
kubectl exec delivery-service-0 -- curl http://localhost:9090/actuator/health/readiness

# Liveness probe
kubectl exec delivery-service-0 -- curl http://localhost:9090/actuator/health/liveness
```

---

## 🔒 Sécurité Production

### Database
- ✅ SSL/TLS activé
- ✅ Credentials depuis Secrets Manager
- ✅ Connection pooling optimisé
- ✅ Read replicas pour scaling

### Kafka
- ✅ SASL/SSL authentication
- ✅ ACL configurées
- ✅ Encryption in transit
- ✅ Consumer groups avec offset management

### Network
- ✅ Port 9090 (Actuator) : Interne uniquement
- ✅ Port 8085 : Via API Gateway uniquement
- ✅ Network policies Kubernetes

---

## 📊 Monitoring & Alerting

### Prometheus Metrics

```bash
# Requêtes HTTP
http_server_requests_seconds_count
http_server_requests_seconds_sum

# Kafka metrics
kafka_producer_record_send_total
kafka_consumer_records_consumed_total

# JVM metrics
jvm_memory_used_bytes
jvm_gc_pause_seconds
```

### Grafana Dashboards

- Delivery Service Overview
- Kafka Consumer Lag
- JVM Metrics
- Database Connection Pool

### Alerting (PagerDuty)

- Service down (> 2 minutes)
- High error rate (> 1%)
- Database connection issues
- Kafka consumer lag (> 10000)
- High response time (p95 > 1s)

---

## 🚨 Disaster Recovery

### Backup

```bash
# Database backup (automatique quotidien)
pg_dump speedline_delivery_prod > backup.sql

# Kafka offset backup
kafka-consumer-groups --bootstrap-server kafka:9092 --describe --group delivery-service
```

### Recovery

```bash
# Restore database
psql speedline_delivery_prod < backup.sql

# Reset Kafka offsets (si nécessaire)
kafka-consumer-groups --bootstrap-server kafka:9092 \
  --group delivery-service --reset-offsets --to-earliest --execute --topic delivery.created
```

---

## 📝 Checklist Déploiement Production

- [ ] Code review complété
- [ ] Tests unitaires/intégration réussis
- [ ] Build Docker réussi
- [ ] Image scannée (Trivy)
- [ ] Secrets Manager configuré
- [ ] Database migrations testées
- [ ] Kafka topics créés
- [ ] Monitoring et alertes configurés
- [ ] Plan de rollback préparé
- [ ] Runbook de maintenance créé
- [ ] Équipe support formée
- [ ] Déploiement validé
- [ ] Health checks verts
- [ ] Tests de charge réussis

---

## 📈 SLA & Performance

### SLA Objectifs
- **Availability** : 99.95%
- **Response Time** : < 500ms (p95)
- **Error Rate** : < 0.1%
- **Kafka Lag** : < 1000 messages

### Scaling
```bash
# Scaler horizontalement
kubectl scale deployment delivery-service-prod --replicas=5 -n production

# Auto-scaling (HPA)
kubectl autoscale deployment delivery-service-prod \
  --cpu-percent=70 --min=3 --max=10 -n production
```

---

**Profil** : Production (CRITIQUE)  
**Port** : 8085  
**⚠️** Document de référence pour production

