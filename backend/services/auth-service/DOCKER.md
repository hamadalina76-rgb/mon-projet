# 🐳 GUIDE DOCKER - AUTH SERVICE

## 📋 Dockerfiles Disponibles

### Dockerfile.dev (Développement)
- **Usage** : Développement local avec Docker
- **Optimisations** : Build basique, debugging activé
- **Taille** : ~250 MB

### Dockerfile.staging (Pré-production)
- **Usage** : Tests en environnement staging
- **Optimisations** : JVM tuning moyen, healthcheck amélioré
- **Taille** : ~250 MB

### Dockerfile.prod (Production)
- **Usage** : Déploiement production
- **Optimisations** : JVM tuning optimal, sécurité maximale
- **Taille** : ~250 MB

---

## 🚀 Commandes de Build

### Développement
```bash
# Builder l'image
docker build -f Dockerfile.dev -t speedline/auth-service:dev .

# Lancer le conteneur
docker run -p 8081:8081 -p 9090:9090 \
  --env-file .env.dev \
  --name auth-service-dev \
  speedline/auth-service:dev

# Avec logs
docker logs -f auth-service-dev
```

### Staging
```bash
# Builder l'image
docker build -f Dockerfile.staging -t speedline/auth-service:staging .

# Lancer le conteneur
docker run -p 8081:8081 -p 9090:9090 \
  --env-file .env.staging \
  --network speedline-network \
  --name auth-service-staging \
  speedline/auth-service:staging
```

### Production
```bash
# Builder l'image
docker build -f Dockerfile.prod -t speedline/auth-service:prod .

# Tagger pour registry
docker tag speedline/auth-service:prod \
  gcr.io/speedline-prod/auth-service:1.0.0

# Pousser vers registry
docker push gcr.io/speedline-prod/auth-service:1.0.0

# Déployer sur Kubernetes
kubectl apply -f k8s/auth-service-prod.yml
```

---

## 🔍 Inspection de l'Image

```bash
# Voir les layers
docker history speedline/auth-service:dev

# Inspecter l'image
docker inspect speedline/auth-service:dev

# Scanner les vulnérabilités
docker scan speedline/auth-service:dev
# ou avec Trivy
trivy image speedline/auth-service:dev
```

---

## 🏥 Health Checks

### Vérifier la santé du conteneur
```bash
# Health check status
docker ps --filter name=auth-service-dev --format "{{.Status}}"

# Health endpoint
curl http://localhost:9090/actuator/health

# Readiness probe
curl http://localhost:9090/actuator/health/readiness

# Liveness probe
curl http://localhost:9090/actuator/health/liveness
```

---

## 📊 Monitoring

### Logs
```bash
# Voir les logs en temps réel
docker logs -f auth-service-dev

# Dernières 100 lignes
docker logs --tail 100 auth-service-dev

# Logs avec timestamp
docker logs --timestamps auth-service-dev
```

### Ressources
```bash
# Utilisation ressources
docker stats auth-service-dev

# Inspecter le conteneur
docker exec -it auth-service-dev sh
```

---

## 🔧 Troubleshooting

### Problème : Le build échoue
```bash
# Nettoyer le cache Docker
docker builder prune -a

# Builder sans cache
docker build --no-cache -f Dockerfile.dev -t speedline/auth-service:dev .
```

### Problème : Conteneur ne démarre pas
```bash
# Voir les logs d'erreur
docker logs auth-service-dev

# Vérifier les variables d'environnement
docker exec auth-service-dev env

# Tester la connexion BD
docker exec auth-service-dev nc -zv postgres 5432
```

### Problème : Health check échoue
```bash
# Tester manuellement
docker exec auth-service-dev curl http://localhost:9090/actuator/health

# Vérifier les ports
docker port auth-service-dev
```

---

## 🌐 Docker Compose

### Créer docker-compose.yml
```yaml
version: '3.8'

services:
  auth-service:
    build:
      context: .
      dockerfile: Dockerfile.dev
    ports:
      - "8081:8081"
      - "9090:9090"
    env_file:
      - .env.dev
    depends_on:
      - postgres
      - redis
    networks:
      - speedline-network

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: speedline_auth
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - speedline-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - speedline-network

networks:
  speedline-network:
    driver: bridge

volumes:
  postgres-data:
```

### Lancer avec Docker Compose
```bash
# Démarrer tous les services
docker-compose up -d

# Voir les logs
docker-compose logs -f auth-service

# Arrêter
docker-compose down

# Arrêter et supprimer les volumes
docker-compose down -v
```

---

## 📝 Best Practices

✅ **Multi-stage builds** : Réduire la taille de l'image finale
✅ **Non-root user** : Sécurité accrue
✅ **Health checks** : Détection automatique des problèmes
✅ **.dockerignore** : Build plus rapide
✅ **Labels** : Métadonnées pour organisation
✅ **Variables d'env** : Configuration flexible
✅ **Alpine base** : Images légères

---

**Version** : 1.0.0  
**Service** : auth-service  
**Port** : 8081 (app) + 9090 (actuator)

