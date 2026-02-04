# 🚀 SpeedLine - Plateforme de Livraison de Repas

## 📋 Guide pour les Stagiaires

Bienvenue dans le projet SpeedLine ! Ce document vous guidera dans la compréhension de l'architecture et l'implémentation des microservices.

---

## 🎯 Vue d'ensemble de l'Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENT APPLICATIONS                     │
│     Flutter Mobile (Customer) | Flutter Mobile (Courier)    │
│         Angular Web (Partner) | Angular Web (Admin)         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   API GATEWAY (Port: 8080)                  │
│   Authentication | Rate Limiting | Load Balancing | CORS    │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Eureka     │  │Config Server │  │ Auth Service │
│  Port: 8761  │  │ Port: 8888   │  │ Port: 8081   │
└──────────────┘  └──────────────┘  └──────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    BUSINESS MICROSERVICES                   │
├──────────────┬──────────────┬──────────────┬───────────────┤
│ User Service │Partner Svc   │Order Service │Delivery Svc   │
│ Port: 8082   │Port: 8083    │Port: 8084    │Port: 8085     │
├──────────────┼──────────────┼──────────────┼───────────────┤
│Payment Svc   │Notification  │Location Svc  │Analytics Svc  │
│Port: 8086    │Port: 8087    │Port: 8088    │Port: 8089     │
├──────────────┼──────────────┼──────────────┼───────────────┤
│Promotion Svc │Review Service│Support Svc   │               │
│Port: 8092    │Port: 8091    │Port: 8093    │               │
└──────────────┴──────────────┴──────────────┴───────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  MESSAGE BROKER (Kafka)                     │
│              Event-Driven Communication                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Technologies Utilisées

| Catégorie | Technologies |
|-----------|-------------|
| **Backend** | Java 17, Spring Boot 3.2.0, Spring Cloud 2023.0.0 |
| **Base de Données** | PostgreSQL (PostGIS), MongoDB, Redis, ClickHouse |
| **Messaging** | Apache Kafka |
| **Service Discovery** | Netflix Eureka |
| **API Gateway** | Spring Cloud Gateway |
| **Sécurité** | JWT (jjwt 0.12.3), Spring Security |
| **Communication** | OpenFeign, WebSocket |
| **Intégrations** | Stripe, Firebase FCM, Twilio, SendGrid, Mapbox |

---

## 📁 Structure du Projet

```
backend/
├── pom.xml                    # Parent POM (dépendances communes)
├── eureka-server/             # Service Discovery (Port: 8761)
├── config-server/             # Configuration centralisée (Port: 8888)
├── api-gateway/               # Gateway + Auth Filter (Port: 8080)
└── services/
    ├── auth-service/          # Authentification JWT (Port: 8081)
    ├── user-service/          # Gestion utilisateurs (Port: 8082)
    ├── partner-service/       # Restaurants/Magasins (Port: 8083)
    ├── order-service/         # Commandes (Port: 8084)
    ├── delivery-service/      # Livraisons (Port: 8085)
    ├── payment-service/       # Paiements Stripe (Port: 8086)
    ├── notification-service/  # Push/SMS/Email (Port: 8087)
    ├── location-service/      # Géolocalisation (Port: 8088)
    ├── analytics-service/     # Analytiques (Port: 8089)
    ├── promotion-service/     # Promotions (Port: 8092)
    ├── review-service/        # Avis (Port: 8091)
    └── support-service/       # Support (Port: 8093)
```

---

## 🚀 Comment Démarrer

> **📖 Guide Complet :** Consultez le fichier **[GUIDE-DEMARRAGE.md](./GUIDE-DEMARRAGE.md)** pour un guide détaillé étape par étape destiné aux stagiaires.

### Démarrage Rapide

#### Option 1 : Script PowerShell (Recommandé pour Windows)

```powershell
# Démarrer tous les services automatiquement
.\start-all-services.ps1

# Arrêter tous les services
.\stop-all-services.ps1
```

#### Option 2 : Démarrage Manuel

1. **Démarrer l'infrastructure Docker** (obligatoire en premier)
   ```powershell
   docker-compose up -d postgres mongodb redis clickhouse kafka zookeeper kafka-ui eureka-server
   ```

2. **Démarrer les microservices** (dans l'ordre recommandé)
   ```powershell
   # Services de base
   cd services\auth-service && mvn spring-boot:run
   cd services\user-service && mvn spring-boot:run
   cd services\partner-service && mvn spring-boot:run
   
   # Services métier
   cd services\location-service && mvn spring-boot:run
   cd services\payment-service && mvn spring-boot:run
   cd services\promotion-service && mvn spring-boot:run
   cd services\order-service && mvn spring-boot:run
   cd services\delivery-service && mvn spring-boot:run
   
   # Services auxiliaires
   cd services\notification-service && mvn spring-boot:run
   cd services\review-service && mvn spring-boot:run
   cd services\support-service && mvn spring-boot:run
   cd services\analytics-service && mvn spring-boot:run
   
   # API Gateway (en dernier)
   cd ..\..\api-gateway && mvn spring-boot:run
   ```

### Prérequis
- Java 17+
- Maven 3.8+
- Docker Desktop (Windows/Mac) ou Docker (Linux)
- Git

---

## 📝 Guide d'Implémentation par Service

### Convention de Package

Chaque microservice doit suivre cette structure :

```
com.speedline.<service>/
├── <Service>Application.java    # Point d'entrée @SpringBootApplication
├── domain/                       # Entités JPA/MongoDB
├── repository/                   # Interfaces Repository
├── service/                      # Logique métier
├── controller/                   # REST Controllers
├── dto/                          # Data Transfer Objects
│   ├── request/                  # Requêtes entrantes
│   └── response/                 # Réponses sortantes
├── exception/                    # Exceptions personnalisées
├── config/                       # Configurations
├── client/                       # Clients Feign (appels inter-services)
└── event/                        # Kafka events
    ├── producer/                 # Producteurs d'événements
    └── consumer/                 # Consommateurs d'événements
```

---

## 🔧 Détails par Microservice

### 1. Auth Service (Port: 8081)

**Responsabilités:**
- Authentification JWT
- Inscription/Login/Logout
- Refresh tokens
- Validation email/SMS
- Reset password

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/auth/register` | Inscription |
| POST | `/auth/login` | Connexion |
| POST | `/auth/refresh` | Refresh token |
| POST | `/auth/logout` | Déconnexion |
| POST | `/auth/verify-email` | Vérifier email |
| POST | `/auth/forgot-password` | Mot de passe oublié |
| POST | `/auth/reset-password` | Réinitialiser |
| GET | `/auth/validate-token` | Valider JWT |

**Base de données:** PostgreSQL
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(50) NOT NULL, -- CUSTOMER, COURIER, PARTNER, ADMIN
    status VARCHAR(50) NOT NULL,
    is_email_verified BOOLEAN DEFAULT FALSE,
    is_phone_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL
);
```

---

### 2. User Service (Port: 8082)

**Responsabilités:**
- Gestion profils (Customer, Courier)
- Gestion adresses
- Préférences utilisateur

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/customers/{id}` | Détails client |
| PUT | `/customers/{id}` | Modifier client |
| GET | `/customers/{id}/addresses` | Adresses client |
| POST | `/customers/{id}/addresses` | Ajouter adresse |
| GET | `/couriers/{id}` | Détails livreur |
| PUT | `/couriers/{id}/availability` | Disponibilité |

**Base de données:** PostgreSQL
```sql
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    wallet_balance DECIMAL(10,2) DEFAULT 0,
    loyalty_points INTEGER DEFAULT 0,
    preferences JSONB
);

CREATE TABLE couriers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    vehicle_type VARCHAR(50),
    current_location GEOGRAPHY(POINT, 4326),
    status VARCHAR(50) DEFAULT 'IDLE',
    rating DECIMAL(3,2) DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE
);

CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(100),
    street VARCHAR(255),
    city VARCHAR(100),
    location GEOGRAPHY(POINT, 4326),
    is_default BOOLEAN DEFAULT FALSE
);
```

---

### 3. Partner Service (Port: 8083)

**Responsabilités:**
- Gestion restaurants/magasins
- Gestion menu (produits, catégories)
- Disponibilité produits
- Horaires d'ouverture

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/partners` | Liste partenaires |
| GET | `/partners/{id}` | Détails partenaire |
| POST | `/partners` | Créer partenaire |
| GET | `/partners/{id}/menu` | Menu complet |
| GET | `/partners/search` | Recherche |
| POST | `/partners/{id}/products` | Ajouter produit |
| PUT | `/products/{id}/availability` | Disponibilité |

**Base de données:** PostgreSQL
```sql
CREATE TABLE partners (
    id BIGSERIAL PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    location GEOGRAPHY(POINT, 4326),
    delivery_zone GEOGRAPHY(POLYGON, 4326),
    rating DECIMAL(3,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    opening_hours JSONB,
    minimum_order DECIMAL(10,2)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    partner_id BIGINT REFERENCES partners(id),
    category_id BIGINT,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    is_available BOOLEAN DEFAULT TRUE
);
```

---

### 4. Order Service (Port: 8084)

**Responsabilités:**
- Création/gestion commandes
- Validation commandes
- Calcul des prix
- Workflow des statuts

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/orders` | Créer commande |
| GET | `/orders/{id}` | Détails commande |
| PUT | `/orders/{id}/status` | Changer statut |
| DELETE | `/orders/{id}` | Annuler |
| GET | `/orders/{id}/track` | Tracking |

**Statuts de commande:**
```
PENDING → CONFIRMED → PREPARING → READY_FOR_PICKUP → 
PICKED_UP → IN_DELIVERY → DELIVERED
                    ↓
              CANCELLED
```

**Kafka Events à produire:**
- `OrderCreatedEvent`
- `OrderConfirmedEvent`
- `OrderCancelledEvent`
- `OrderCompletedEvent`

---

### 5. Delivery Service (Port: 8085)

**Responsabilités:**
- Matching commande ↔ livreur
- Tracking GPS temps réel
- Calcul routes optimales
- Preuve de livraison

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/deliveries` | Créer livraison |
| PUT | `/deliveries/{id}/accept` | Accepter |
| PUT | `/deliveries/{id}/pickup` | Récupérer |
| PUT | `/deliveries/{id}/complete` | Terminer |
| POST | `/deliveries/{id}/location` | Update GPS |
| WS | `/ws/tracking/{orderId}` | Tracking temps réel |

**Algorithme de matching (à implémenter):**
```java
// Critères de sélection du livreur
1. Distance du livreur au restaurant
2. Disponibilité (is_available = true)
3. Rating du livreur
4. Nombre de livraisons en cours
5. Type de véhicule adapté
```

---

### 6. Payment Service (Port: 8086)

**Responsabilités:**
- Traitement paiements (Stripe)
- Gestion wallets
- Remboursements
- Historique transactions

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/payments/process` | Traiter paiement |
| POST | `/payments/{id}/refund` | Rembourser |
| GET | `/wallets/{userId}` | Solde wallet |
| POST | `/wallets/{userId}/add` | Ajouter fonds |
| POST | `/payment-methods` | Ajouter carte |

**Intégration Stripe:**
```java
// Dépendance: com.stripe:stripe-java:24.0.0
Stripe.apiKey = "sk_test_...";

PaymentIntent intent = PaymentIntent.create(
    PaymentIntentCreateParams.builder()
        .setAmount(amount)
        .setCurrency("tnd")
        .build()
);
```

---

### 7. Notification Service (Port: 8087)

**Responsabilités:**
- Push notifications (FCM)
- SMS (Twilio)
- Emails (SendGrid)
- Templates de notifications

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/notifications/send` | Envoyer |
| GET | `/notifications/{userId}` | Historique |
| PUT | `/notifications/{id}/read` | Marquer lu |
| POST | `/push-tokens` | Enregistrer token |

**Kafka Consumers à implémenter:**
```java
@KafkaListener(topics = "order-created")
public void onOrderCreated(OrderCreatedEvent event) {
    // Notifier client + partenaire
}

@KafkaListener(topics = "delivery-assigned")
public void onDeliveryAssigned(DeliveryAssignedEvent event) {
    // Notifier livreur
}
```

**Base de données:** MongoDB

---

### 8. Location Service (Port: 8088)

**Responsabilités:**
- Calculs géospatiaux (PostGIS)
- Recherche partenaires proches
- Gestion zones de livraison
- Géocodage (Mapbox)

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/locations/nearby-partners` | Partenaires proches |
| POST | `/locations/calculate-distance` | Calculer distance |
| POST | `/locations/geocode` | Adresse → Coordonnées |
| GET | `/zones` | Liste zones |

**Fonction PostGIS:**
```sql
-- Trouver partenaires dans un rayon
SELECT * FROM partners 
WHERE ST_DWithin(
    location,
    ST_SetSRID(ST_MakePoint(lon, lat), 4326),
    5000  -- 5km
);
```

---

### 9. Analytics Service (Port: 8089)

**Responsabilités:**
- Métriques temps réel
- Rapports business
- Dashboard analytics
- Export de données

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/analytics/dashboard` | Métriques dashboard |
| GET | `/analytics/orders/stats` | Stats commandes |
| GET | `/analytics/revenue` | Revenus |
| POST | `/reports/generate` | Générer rapport |

**Base de données:** ClickHouse (optimisé pour analytics)

---

### 10. Promotion Service (Port: 8092)

**Responsabilités:**
- Gestion promotions/coupons
- Validation codes promo
- Programme fidélité

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/promotions/active` | Promos actives |
| POST | `/promotions/validate` | Valider code |
| POST | `/promotions/{code}/apply` | Appliquer |

---

### 11. Review Service (Port: 8091)

**Responsabilités:**
- Gestion avis clients
- Notes partenaires/livreurs
- Modération

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/reviews` | Créer avis |
| GET | `/reviews/partner/{id}` | Avis partenaire |
| PUT | `/reviews/{id}/response` | Répondre |

**Base de données:** MongoDB

---

### 12. Support Service (Port: 8093)

**Responsabilités:**
- Tickets support
- Chat en direct
- Gestion litiges

**Endpoints à implémenter:**
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/tickets` | Créer ticket |
| PUT | `/tickets/{id}/resolve` | Résoudre |
| WS | `/ws/chat/{ticketId}` | Chat direct |

---

## 🔄 Communication Inter-Services

### 1. Appels REST (Synchrone) - OpenFeign

```java
@FeignClient(name = "user-service")
public interface UserServiceClient {
    
    @GetMapping("/customers/{id}")
    CustomerDTO getCustomer(@PathVariable Long id);
}
```

### 2. Events Kafka (Asynchrone)

**Producer:**
```java
@Service
public class OrderEventProducer {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    public void sendOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send("order-created", event);
    }
}
```

**Consumer:**
```java
@Service
public class OrderEventConsumer {
    
    @KafkaListener(topics = "order-created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Traiter l'événement
    }
}
```

### 3. Topics Kafka

| Topic | Producteur | Consommateurs |
|-------|-----------|---------------|
| `order-created` | Order Service | Notification, Delivery, Analytics |
| `order-confirmed` | Order Service | Notification, Partner |
| `order-completed` | Order Service | Notification, Analytics, Review |
| `delivery-assigned` | Delivery Service | Notification |
| `payment-completed` | Payment Service | Notification, Order |

---

## 📊 Bases de Données par Service

| Service | Database | Technologie |
|---------|----------|-------------|
| Auth | speedline_auth | PostgreSQL |
| User | speedline_users | PostgreSQL + PostGIS |
| Partner | speedline_partners | PostgreSQL + PostGIS |
| Order | speedline_orders | PostgreSQL |
| Delivery | speedline_delivery | PostgreSQL + PostGIS |
| Payment | speedline_payments | PostgreSQL |
| Notification | speedline_notifications | MongoDB |
| Location | speedline_locations | PostgreSQL + PostGIS |
| Analytics | speedline_analytics | ClickHouse |
| Promotion | speedline_promotions | PostgreSQL |
| Review | speedline_reviews | MongoDB |
| Support | speedline_support | PostgreSQL |

**Cache partagé:** Redis

---

## ✅ Checklist d'Implémentation

Pour chaque service, suivez cette checklist :

- [ ] **Entités (domain/):** Créer les classes avec annotations JPA/MongoDB
- [ ] **Repository:** Créer les interfaces Repository
- [ ] **Service:** Implémenter la logique métier
- [ ] **Controller:** Créer les endpoints REST
- [ ] **DTOs:** Créer Request/Response DTOs
- [ ] **Exceptions:** Gérer les erreurs métier
- [ ] **Tests:** Écrire les tests unitaires
- [ ] **application.yml:** Configurer le service
- [ ] **Kafka:** Implémenter producers/consumers si nécessaire
- [ ] **Feign Clients:** Créer les clients pour appels inter-services

---

## 🔐 Sécurité

### JWT Token Structure
```json
{
  "sub": "user_id",
  "email": "user@example.com",
  "role": "CUSTOMER",
  "iat": 1234567890,
  "exp": 1234567890
}
```

### Headers requis
```
Authorization: Bearer <jwt_token>
```

---

## 📞 Support

Pour toute question :
- Consultez la documentation Spring Cloud
- Vérifiez les logs avec `mvn spring-boot:run`
- Utilisez Eureka Dashboard : http://localhost:8761

---

**Bon courage ! 💪**

*L'équipe SpeedLine*
