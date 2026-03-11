# 📚 GUIDE PRODUCTION - API GATEWAY SPEEDLINE

## 📋 Table des matières

1. [Configuration Initiale](#configuration-initiale)
2. [Démarrage du Service](#démarrage-du-service)
3. [Variables d'Environnement](#variables-denvironnement)
4. [Fichiers de Configuration](#fichiers-de-configuration)
5. [Architecture des Routes](#architecture-des-routes)
6. [Monitoring & Observabilité](#monitoring--observabilité)
7. [Vérification de la Santé](#vérification-de-la-santé)
8. [Docker Production](#docker-production)
9. [Sécurité & Bonnes Pratiques](#sécurité--bonnes-pratiques)
10. [Troubleshooting](#troubleshooting)

---

## 🚀 Configuration Initiale

### Prérequis
- **Java 17+** (OpenJDK 17)
- **Maven 3.8+**
- **Docker & Docker Compose** (obligatoire)
- **Git**
- **Kubernetes** (optionnel pour orchestration)

### Vérifier l'installation Java
```bash
java -version
# Résultat attendu : openjdk version "17.x.x" ou supérieur
```

### Cloner et compiler le projet
```bash
cd /home/nayer/IdeaProjects/speedline/backend
mvn clean compile
```

---

## ▶️ Démarrage du Service

### ⚠️ IMPORTANT : Production avec Docker uniquement

L'API Gateway **DOIT** être déployée via **Docker** en production.

### 1️⃣ Démarrage avec Docker Compose

```bash
# Charger les variables
source .env.prod

# Builder l'image production
docker build -f Dockerfile.prod -t speedline/api-gateway:prod .

# Lancer avec docker-compose
docker-compose -f docker-compose.prod.yml up -d api-gateway

# Afficher les logs
docker-compose -f docker-compose.prod.yml logs -f api-gateway
```

### 2️⃣ Déploiement Kubernetes

```bash
# Appliquer la configuration Kubernetes
kubectl apply -f k8s/api-gateway-prod.yml

# Vérifier le déploiement
kubectl get pods -n production
kubectl logs -f deployment/api-gateway-prod -n production

# Exposer le service
kubectl port-forward svc/api-gateway-prod 8080:8080 -n production
```

### 3️⃣ Déploiement sur Cloud (GCP, AWS, Azure)

```bash
# Déployer sur Google Cloud Run
gcloud run deploy api-gateway \
  --image gcr.io/speedline-prod/api-gateway:prod \
  --platform managed \
  --region europe-west1 \
  --env-vars-file .env.prod.yaml \
  --cpu 2 \
  --memory 2Gi \
  --max-instances 100

# Ou sur AWS ECS
aws ecs update-service --cluster prod \
  --service api-gateway \
  --force-new-deployment \
  --region eu-west-1
```

---

## 🔐 Variables d'Environnement

### Fichier `.env.prod`

Ce fichier contient toutes les variables d'environnement pour la **production**.

#### ⚠️ SÉCURITÉ : Utiliser un gestionnaire de secrets

**NE JAMAIS** commiter `.env.prod` dans Git !

Utiliser : **AWS Secrets Manager**, **Vault**, **Azure Key Vault**, etc.

#### Variables Principales

| Variable | Valeur Par Défaut | Description |
|----------|------------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Profil Spring actif |
| `REDIS_HOST` | `redis-prod` | Hôte Redis productif |
| `REDIS_PORT` | `6379` | Port Redis |
| `REDIS_PASSWORD` | `${REDIS_PASSWORD_PROD}` | ⚠️ Récupérer depuis Secrets Manager |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | `http://eureka:eureka123@eureka:8761/eureka/` | URL Eureka Server |
| `JWT_SECRET` | `${JWT_SECRET_PROD}` | ⚠️ Clé secrète depuis Secrets Manager |
| `MANAGEMENT_SERVER_PORT` | `9090` | Port Actuator (interne) |

#### Sourcer les variables (Production)
```bash
# ❌ NE PAS utiliser en production
# source .env.prod

# ✅ Utiliser un gestionnaire de secrets
aws secretsmanager get-secret-value --secret-id api-gateway/prod
# ou
vault kv get secret/api-gateway/prod
```

---

## ⚙️ Fichiers de Configuration

### Structure
```
api-gateway/
├── Dockerfile              # Production optimisée
├── Dockerfile.prod         # Alias de production
├── docker-compose.prod.yml # Composition production
├── .env.prod              # Variables production (GIT IGNORED)
├── k8s/
│   ├── api-gateway-prod.yml        # Config Kubernetes
│   ├── api-gateway-service.yml     # Service Kubernetes
│   └── api-gateway-ingress.yml     # Ingress Kubernetes
├── pom.xml                # Dépendances Maven
└── src/main/resources/
    ├── application.yml                # Config par défaut
    ├── application-prod.yml           # Config production (COURANT)
    └── logback-spring.xml             # Configuration Logback
```

### application-prod.yml (Configuration production)
✅ Configuration pour **production** :
- Logs ERROR (minimal)
- Lazy initialization : false (chargement complet)
- Health checks restreints
- Circuit Breaker très strict
- Ressources maximales (500 threads max)
- Optimisations JVM activées

---

## 🗺️ Architecture des Routes

L'API Gateway route les requêtes vers les microservices en production :

### Routes Principales

```
┌─ /api/v1/auth/**          → auth-service (Authentification)
├─ /api/v1/admins/**        → user-service (Gestion admins)
├─ /api/users/**            → user-service (Utilisateurs)
├─ /api/partners/**         → partner-service (Partenaires)
├─ /api/orders/**           → order-service (Commandes)
├─ /api/deliveries/**       → delivery-service (Livraisons)
├─ /api/payments/**         → payment-service (Paiements)
├─ /api/notifications/**    → notification-service (Notifications)
├─ /api/locations/**        → location-service (Localisation)
├─ /api/analytics/**        → analytics-service (Analytiques)
├─ /api/promotions/**       → promotion-service (Promotions)
├─ /api/reviews/**          → review-service (Avis)
└─ /api/tickets/**          → support-service (Support)
```

### Filtres Critiques

| Filtre | Fonctionnalité |
|--------|---------------|
| `StripPrefix=1` | Enlever `/api` avant router |
| `AuthenticationFilter` | Vérifier le JWT (OBLIGATOIRE) |
| `RateLimitingFilter` | Limiter les requêtes par utilisateur |
| `DedupeResponseHeader` | Éviter les headers CORS dupliqués |

---

## 📊 Monitoring & Observabilité

### Configuration de Logging
```bash
# Dans application-prod.yml :
logging:
  level:
    root: ERROR
    org.springframework: WARN
    org.springframework.cloud.gateway: WARN
    com.speedline: WARN
```

### Logs centralisés (Recommandé)
```bash
# Envoyer les logs vers ELK Stack / CloudWatch
# Configuration dans logback-spring.xml
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>logstash:5000</destination>
</appender>
```

### Métriques Prometheus
```bash
# Les métriques sont exposées sur le port 9090
curl http://localhost:9090/actuator/prometheus

# Importer dans Grafana pour les dashboards
# Datasource : http://prometheus:9090
```

### APM (Application Performance Monitoring)
```bash
# Ajouter Datadog / New Relic / Elastic APM
# Configuration via variables d'environnement
```

---

## ✅ Vérification de la Santé

### Health Check (Port 9090 - INTERNE)
```bash
# ⚠️ Accès interne uniquement
curl http://api-gateway:9090/actuator/health
# Réponse : {"status":"UP"}

# Liveness probe (Kubernetes)
curl http://api-gateway:9090/actuator/health/liveness

# Readiness probe (Kubernetes)
curl http://api-gateway:9090/actuator/health/readiness
```

### Afficher les routes configurées
```bash
curl http://api-gateway:9090/actuator/gateway/routes | jq
```

### Vérifier les métriques
```bash
curl http://api-gateway:9090/actuator/metrics | jq
curl http://api-gateway:9090/actuator/metrics/http.server.requests
```

---

## 🐳 Docker Production

### Builder l'image production (Multi-stage)
```bash
docker build -f Dockerfile.prod -t speedline/api-gateway:prod .

# Tagger pour registry
docker tag speedline/api-gateway:prod gcr.io/speedline-prod/api-gateway:prod

# Pousser vers registry
docker push gcr.io/speedline-prod/api-gateway:prod
```

### Lancer avec docker-compose
```bash
docker-compose -f docker-compose.prod.yml up -d api-gateway

# Vérifier le statut
docker ps | grep api-gateway

# Logs
docker-compose -f docker-compose.prod.yml logs -f api-gateway
```

### Scaling horizontal
```bash
# Scaler le service (Docker Swarm)
docker service scale api-gateway=5

# Ou avec Kubernetes
kubectl scale deployment api-gateway-prod --replicas=5 -n production
```

---

## 🔐 Sécurité & Bonnes Pratiques

### 1. Authentification & Autorisation
- ✅ JWT tokens obligatoires
- ✅ Tokens signés avec clé RSA 2048
- ✅ Refresh tokens après expiration
- ✅ Rate limiting par utilisateur

### 2. HTTPS/TLS
```bash
# Activer HTTPS
server:
  ssl:
    key-store: /secrets/keystore.jks
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: JKS
    key-alias: tomcat
```

### 3. Secrets Management
```bash
# ✅ Utiliser AWS Secrets Manager
aws secretsmanager get-secret-value \
  --secret-id api-gateway/prod \
  --region eu-west-1

# ✅ Ou Kubernetes Secrets
kubectl get secret api-gateway-prod -n production -o yaml
```

### 4. Network Policies
```bash
# Restreindre l'accès au port 9090 (Actuator)
# Règles firewall : Autoriser 8080 publiquement, 9090 interne uniquement
```

### 5. Sauvegarde & Disaster Recovery
```bash
# Sauvegarder les configurations
kubectl get cm,secret -n production -o yaml > backup.yml

# Plan de récupération en cas de panne
# RTO : Recovery Time Objective : < 15 minutes
# RPO : Recovery Point Objective : < 5 minutes
```

---

## 🔧 Troubleshooting

### Problème : Service ne démarre pas
```bash
# Vérifier les logs
docker-compose -f docker-compose.prod.yml logs api-gateway

# Vérifier les ressources
docker stats api-gateway

# Vérifier la connectivité réseau
docker network ls
docker network inspect speedline-network
```

### Problème : Health check échoue
```bash
# Vérifier l'accès interne
docker exec api-gateway curl http://localhost:9090/actuator/health

# Vérifier les dépendances (Redis, Eureka)
docker ps | grep -E "redis|eureka"

# Vérifier la connectivité
docker logs api-gateway | grep -i "connection\|error"
```

### Problème : Haute latence
```bash
# Vérifier les métriques
curl http://api-gateway:9090/actuator/metrics/http.server.requests

# Vérifier les logs de temps de réponse
docker logs api-gateway | grep "took\|duration"

# Vérifier le circuit breaker
curl http://api-gateway:9090/actuator/metrics/resilience4j.circuitbreaker
```

### Problème : Fuite mémoire
```bash
# Monitorer la mémoire
docker stats --no-stream api-gateway

# Dump heap
kubectl exec api-gateway-prod -- jmap -dump:live,format=b,file=/tmp/heap.bin 1

# Analyser avec Eclipse MAT ou JProfiler
```

---

## 📝 Checklist Déploiement Production

- [ ] Code review complété
- [ ] Tests unitaires/intégration réussis
- [ ] Build Docker réussi
- [ ] Image scannée pour vulnérabilités (Trivy)
- [ ] Secrets Manager configuré
- [ ] HTTPS/TLS activé
- [ ] Monitoring et alertes configurés
- [ ] Plan de rollback préparé
- [ ] Runbook de maintenance créé
- [ ] Équipe support formée
- [ ] Déploiement en production validé
- [ ] Health checks verts
- [ ] Tests de charge réussis
- [ ] Documentation mise à jour

---

## 📊 SLA & Monitoring

### SLA Objectifs
- **Availability** : 99.95%
- **Response Time** : < 500ms (p95)
- **Error Rate** : < 0.1%

### Monitoring & Alerting
- Prometheus pour les métriques
- Grafana pour les dashboards
- PagerDuty pour les alertes critiques
- Slack pour les notifications

---

## 📚 Ressources

- **Spring Cloud Gateway** : https://spring.io/projects/spring-cloud-gateway
- **Spring Boot Production** : https://spring.io/guides/gs/spring-boot-docker/
- **Kubernetes Deployment** : https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
- **Docker Security** : https://docs.docker.com/engine/security/
- **JWT Security** : https://tools.ietf.org/html/rfc7519

---

**Dernière mise à jour** : 2026-02-23  
**Auteur** : GitHub Copilot  
**Version** : 1.0.0  
**Profil** : Production  
**⚠️ CRITIQUE** : Document de référence pour la production

