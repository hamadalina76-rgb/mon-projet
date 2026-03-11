# 📚 GUIDE PRODUCTION - CONFIG SERVER SPEEDLINE

## 📋 Table des matières

1. [Configuration Initiale](#configuration-initiale)
2. [Démarrage du Service](#démarrage-du-service)
3. [Variables d'Environnement](#variables-denvironnement)
4. [Git Backend (Recommandé)](#git-backend-recommandé)
5. [Docker Production](#docker-production)
6. [Kubernetes Deployment](#kubernetes-deployment)
7. [Sécurité](#sécurité)
8. [Monitoring](#monitoring)
9. [Troubleshooting](#troubleshooting)

---

## 🚀 Configuration Initiale

### Prérequis
- **Java 17+** (OpenJDK 17)
- **Maven 3.8+**
- **Docker** (obligatoire)
- **Kubernetes** (recommandé)
- **Git Repository** (pour configurations centralisées)

---

## ▶️ Démarrage du Service

### ⚠️ Production avec Docker UNIQUEMENT

```bash
# Charger les secrets
source .env.prod

# Builder l'image production
docker build -f Dockerfile.prod -t speedline/config-server:prod .

# Lancer le conteneur
docker run -p 8888:8888 -p 9090:9090 \
  --env-file .env.prod \
  --network speedline-network \
  speedline/config-server:prod
```

---

## 🔐 Variables d'Environnement

### Fichier `.env.prod` - SÉCURISÉ

⚠️ **NE JAMAIS commiter dans Git !**

Utiliser : **AWS Secrets Manager**, **Vault**, **Azure Key Vault**

| Variable | Source | Description |
|----------|--------|-------------|
| `CONFIG_USER` | Secrets Manager | Utilisateur production |
| `CONFIG_PASSWORD` | Secrets Manager | Mot de passe production |
| `GIT_REPO_URI` | Secrets Manager | Repo Git des configurations |
| `GIT_USERNAME` | Secrets Manager | Utilisateur Git |
| `GIT_PASSWORD` | Secrets Manager | Token Git |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | Secrets Manager | Eureka URL |

---

## 📘 Git Backend (Recommandé)

En production, utiliser un **Git Repository** pour gérer les configurations :

### Avantages
- ✅ Versionning des configurations
- ✅ Audit trail complet
- ✅ Rollback facile
- ✅ Haute disponibilité

### Configuration

```yaml
# application-prod.yml
spring:
  profiles:
    active: git
  cloud:
    config:
      server:
        git:
          uri: ${GIT_REPO_URI}
          username: ${GIT_USERNAME}
          password: ${GIT_PASSWORD}
          clone-on-start: true
          force-pull: true
```

### Exemple de structure Git

```
config-repo/
├── api-gateway-dev.yml
├── api-gateway-staging.yml
├── api-gateway-prod.yml
├── auth-service-dev.yml
├── auth-service-staging.yml
├── auth-service-prod.yml
└── ...
```

---

## 🐳 Docker Production

### Builder l'image
```bash
docker build -f Dockerfile.prod -t speedline/config-server:prod .

# Tagger pour registry
docker tag speedline/config-server:prod \
  gcr.io/speedline-prod/config-server:prod

# Pousser vers registry
docker push gcr.io/speedline-prod/config-server:prod
```

### Lancer avec scaling
```bash
# Docker Swarm
docker service create --name config-server \
  --env-file .env.prod \
  --publish 8888:8888 \
  --replicas 3 \
  gcr.io/speedline-prod/config-server:prod
```

---

## ☸️ Kubernetes Deployment

```bash
# Appliquer la configuration
kubectl apply -f k8s/config-server-prod.yml

# Vérifier le déploiement
kubectl get pods -n production
kubectl logs -f deployment/config-server-prod -n production

# Exposer le service
kubectl port-forward svc/config-server-prod 8888:8888 -n production
```

### ConfigMap Kubernetes

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: config-server-env
  namespace: production
data:
  SPRING_PROFILES_ACTIVE: "prod"
  CONFIG_SERVER_GIT_CLONE_ON_START: "true"
```

---

## 🔐 Sécurité

### 1. Authentification
- ✅ Credentials forts (AWS Secrets Manager)
- ✅ Rotation régulière des passwords
- ✅ Audit des accès

### 2. Données sensibles
- ✅ Chiffrer les configurations sensibles en Git
- ✅ Utiliser Spring Cloud Config Encryption
- ✅ RBAC dans Kubernetes

### 3. Accès réseau
- ✅ Port 9090 (Actuator) : Interne uniquement
- ✅ Port 8888 : Restreint aux microservices
- ✅ HTTPS/TLS obligatoire

---

## 📊 Monitoring

### Health Check
```bash
# Interne uniquement
curl http://config-server:9090/actuator/health
```

### Métriques
```bash
curl http://config-server:9090/actuator/metrics
```

### Logs
```bash
# Via Kubernetes
kubectl logs -f deployment/config-server-prod -n production

# Centralisé (ELK Stack / CloudWatch)
# Configurer dans logback-spring.xml
```

---

## 🔧 Troubleshooting

### Problème : Git clone échoue
```bash
# Vérifier les credentials Git
# Vérifier l'accès réseau au repo Git
# Vérifier le token/password en Secrets Manager
```

### Problème : Configurations non rechargées
```bash
# Forcer le rechargement
curl -X POST http://microservice:8080/actuator/refresh

# Ou redémarrer le config server
kubectl rollout restart deployment/config-server-prod -n production
```

### Problème : Haute latence
```bash
# Augmenter les replicas
kubectl scale deployment config-server-prod --replicas=5 -n production

# Ajouter un cache
# Configuration : git.refresh-rate
```

---

## 📝 Checklist Production

- [ ] Secrets Manager configuré
- [ ] Git Repository avec configurations
- [ ] Docker image scannée (Trivy)
- [ ] Kubernetes manifests validés
- [ ] HTTPS/TLS activé
- [ ] Health checks configurés
- [ ] Monitoring & alertes en place
- [ ] Backup & recovery plan
- [ ] Runbooks créés
- [ ] Équipe support formée

---

**Dernière mise à jour** : 2026-02-23  
**Profil** : Production (CRITIQUE)  
**⚠️** Document de référence pour production

