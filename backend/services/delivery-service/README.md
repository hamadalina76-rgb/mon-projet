## 🚚 Delivery Service – Realtime Courier Tracking (WebSocket)

### 1. Objectif

Implémenter un tracking temps réel type Glovo : le livreur envoie sa position via une connexion WebSocket persistante, le backend met à jour Redis + publie un évènement interne, et les clients de suivi consomment la position via un canal `ws://host/ws/tracking/{orderId}`.

---

### 2. Endpoints WebSocket

#### 2.1. Livreur → Backend

- **URL** : `ws://<gateway-host>/ws/location`
- **Client** : `courier_app`
- **Auth** : JWT
  - Pendant le handshake, `WebSocketAuthInterceptor` lit le header `Authorization: Bearer <token>` (ou le paramètre `token` en query si nécessaire côté mobile).
  - Le token est validé via `JwtUtil` (payload décodé, expiration contrôlée).
  - `userId` et `role` sont extraits et stockés dans les attributs de session WebSocket (`courierId`).

- **Message entrant** :

```json
{
  "type": "POSITION_UPDATE",
  "payload": {
    "lat": 36.8423,
    "lng": 10.1937,
    "accuracy": 5.0,
    "speed": 12.3,
    "heading": 180.0,
    "batteryLevel": 87,
    "timestamp": "2026-03-13T13:45:00Z"
  }
}
```

- **Traitement dans `TrackingWebSocketHandler`** :
  - Parse JSON → `PositionUpdateMessage(type, payload)` + `PositionPayload(...)` (records Java 17).
  - Vérifie `type == POSITION_UPDATE`.
  - Met à jour Redis :
    - `courier:{courierId}:position` → JSON de `PositionPayload` (TTL ~30s).
    - `courier:{courierId}:isOnline` → `"true"` (TTL ~30s).
  - Publie un évènement interne :
    - `CourierPositionUpdatedEvent(courierId, payload)` via `ApplicationEventPublisher`.

> La map `courierId -> WebSocketSession` garantit **une seule connexion active** par livreur (l’ancienne session est fermée proprement).

---

#### 2.2. Backend → Clients de tracking

- **URL** : `ws://<gateway-host>/ws/tracking/{orderId}`
- **Client** : `customer_app`, `partner-dashboard`, `admin-panel`, etc.
- **Auth** : même intercepteur JWT (`WebSocketAuthInterceptor`).

- **Abonnement** :
  - `TrackingBroadcastWebSocketHandler` extrait `orderId` à partir du chemin (`/ws/tracking/{orderId}`) et enregistre la session dans `sessionsByOrderId`.

- **Diffusion** :
  - `TrackingBroadcastWebSocketHandler` écoute les `CourierPositionUpdatedEvent`.
  - Pour chaque évènement, il envoie aux sessions connectées un message de la forme :

```json
{
  "type": "COURIER_POSITION",
  "payload": {
    "courierId": "123",
    "lat": 36.8423,
    "lng": 10.1937,
    "heading": 180.0,
    "estimatedArrivalMin": null
  }
}
```

- **Mapping courier → order** :
  - L’implémentation actuelle ne filtre pas encore par `orderId` (prochaine étape : utiliser `TrackingService` / `Delivery` pour router la position vers les bons `orderId`).

---

### 3. États dans Redis

- `courier:{courierId}:position` :
  - Contient le JSON de `PositionPayload` (lat, lng, accuracy, speed, heading, batteryLevel, timestamp).
  - TTL ~30 secondes.
- `courier:{courierId}:isOnline` :
  - `"true"` si une position a été reçue récemment.
  - TTL ~30 secondes.

> Quand le livreur est hors couverture ou ferme l’app, plus aucune position n’est reçue → les clés expirent → le livreur devient **offline** (TC‑14).

---

### 4. Comment tester manuellement

#### 4.1. Pré‑requis

- Démarrer l’infra avec Docker Compose (`backend/docker-compose.yml`) pour Postgres + Redis.
- Démarrer au minimum :
  - `auth-service`
  - `delivery-service`
  - `api-gateway`

- Créer un compte **livreur** et récupérer un **JWT** (login + OTP via `auth-service`).

#### 4.2. Tester `/ws/location` avec un client WebSocket

1. Ouvrir un client WebSocket (Insomnia, Postman, wscat, etc.) :

   - URL : `ws://localhost:8080/ws/location?token=<ACCESS_TOKEN>`

2. Une fois connecté, envoyer :

```json
{
  "type": "POSITION_UPDATE",
  "payload": {
    "lat": 36.8423,
    "lng": 10.1937,
    "accuracy": 5.0,
    "speed": 12.3,
    "heading": 180.0,
    "batteryLevel": 87,
    "timestamp": "2026-03-13T13:45:00Z"
  }
}
```

3. Vérifier Redis dans le conteneur :

```bash
docker exec -it redis redis-cli
KEYS courier:*
GET courier:<courierId>:position
GET courier:<courierId>:isOnline
TTL courier:<courierId>:position
```

Tu dois voir la position et `isOnline=true` avec un TTL proche de 30s.

#### 4.3. Tester `/ws/tracking/{orderId}`

1. Ouvrir un second client WebSocket :

   - URL : `ws://localhost:8080/ws/tracking/12345?token=<ACCESS_TOKEN_CLIENT>`

2. Laisser cette connexion ouverte.
3. Depuis le client livreur (ou Postman), renvoyer un `POSITION_UPDATE`.
4. Le client tracking doit recevoir un message `COURIER_POSITION` contenant `courierId`, `lat`, `lng`, `heading`, `estimatedArrivalMin`.

#### 4.4. Tester l’état hors ligne (TTL / 15s+)

1. Envoyer quelques `POSITION_UPDATE`.
2. Arrêter les envois ou fermer le WebSocket livreur.
3. Attendre > 30s.

4. Vérifier que les clés `courier:{courierId}:position` et `courier:{courierId}:isOnline` ont expiré dans Redis → le livreur est hors ligne.

### 5. Pré-requis Redis pour DISP-103

Le filtrage interne/externe et la pré-assignation de DISP-103 s’appuient sur des clés Redis écrites par `location-service`.

- `courier:{id}:type` : `INTERNAL` ou `EXTERNAL`
- `courier:{id}:shiftStart` / `courier:{id}:shiftEnd` : heure ISO `HH:mm`
- `courier:{id}:vehicleType` : type de véhicule
- `courier:{id}:currentDelivery:etaFinish` : `Instant` ISO-8601 pour la pré-assignation

Si ces clés sont absentes, le moteur de dispatch applique des valeurs sûres par défaut, mais la priorisation DISP-103 ne peut pas être validée complètement sans elles.

### DISP-205 — Admin dispatch configuration

- **REST** (via API gateway `StripPrefix` → service): `GET/PUT /dispatch/dispatch-config/{general|scoring|internal-external|bundling|exclusivity}`, `POST /dispatch/dispatch-config/simulate`, `GET /dispatch/dispatch-config/replay/{cycleId}`, `GET /dispatch/dispatch-config/audit/export`.
- **Persistence**: Flyway `V5__disp205_dispatch_admin_config.sql` — versions/snapshots, audit, simulation runs, cycle captures + replay pair scores, exclusivity tables.
- **Redis**: atomic publish via `DispatchConfigRuntimeWriter` — keys `dispatch:config:version`, `dispatch:cost:components` (legacy array preserved), `dispatch:config:general`, `dispatch:config:internal-external`, `dispatch:config:bundling`, `dispatch:config:exclusivity`.
- **Runtime**: `RuntimeDispatchTuningService` merges Redis JSON with YAML for bundling + response timeout + lock TTL each cycle; `CostFunctionService` reads scoring from Redis with versioned cache invalidation.
- **Security**: `DispatchConfigManageSecurityFilter` requires `X-User-Role` `ADMIN` or `SUPER_ADMIN` (align with admin-panel `delivery:manage` until JWT permissions are forwarded).
- **Admin UI**: Angular route `/dispatch/config` (permission `delivery:manage`), six tabs + state service + API client extensions.

