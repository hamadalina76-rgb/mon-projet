# Cart Screen - Notes d'implementation

## 1) Objectif du ticket
Mettre en place un ecran Panier complet cote customer app avec:
- architecture 2 couches (etat Riverpod + sync backend),
- UI complete du panier,
- bouton panier global (FAB) visible dans le shell principal,
- integration depuis la fiche partenaire (ajout au panier),
- route dediee dans le router.

## 2) Architecture implementee

### Couche 1 - Etat (Riverpod)
- `CartState`:
  - `items`, `isLoading`, `hasPendingSync`
  - derives: `subtotal`, `total`, `itemCount`, `partnerId`
- `CartNotifier`:
  - hydrate depuis le backend au demarrage,
  - add/update/remove/clear,
  - gestion regle mono-partenaire (dialog de confirmation si changement de partenaire),
  - debounce de sync backend (300ms),
  - retry de sync pending au retour foreground,
  - sync server au login,
  - checkout via `/api/orders`.

Fichiers:
- `lib/features/cart/domain/cart_notifier.dart`
- `lib/features/cart/cart_providers.dart`

### Couche 2 (cote mobile) - Sync vers backend
Important:
- cette couche est cote Flutter (Dart),
- elle n'implemente pas le backend,
- elle fait seulement les appels HTTP vers les API backend.

- `CartRepository`:
  - GET panier serveur (`/api/cart`),
  - PATCH panier serveur (`/api/cart`),
  - infos partenaire (`/api/partners/{id}`),
  - frais livraison (`/api/partners/{id}/delivery-fee`),
  - validation promo (plusieurs endpoints tries),
  - creation commande (`/api/orders`).

Fichier:
- `lib/features/cart/data/repositories/cart_repository.dart`

### Backend implemente
- service panier expose depuis order-service: `GET /cart`, `PATCH /cart`, `DELETE /cart/clear`,
- stockage panier dans Redis cote serveur,
- TTL Redis borne entre 24h et 72h (3 jours max),
- route gateway active: `/api/cart` et `/api/cart/**` vers order-service.

Fichiers backend ajoutes/modifies:
- `backend/services/order-service/src/main/java/com/speedline/order/controller/CartController.java`
- `backend/services/order-service/src/main/java/com/speedline/order/service/CartService.java`
- `backend/services/order-service/src/main/java/com/speedline/order/service/impl/CartServiceImpl.java`
- `backend/services/order-service/src/main/java/com/speedline/order/dto/cart/CartPatchRequest.java`
- `backend/services/order-service/src/main/java/com/speedline/order/dto/cart/CartItemPayload.java`
- `backend/services/order-service/src/main/java/com/speedline/order/dto/cart/CartResponse.java`
- `backend/services/order-service/src/main/resources/application.yml`
- `backend/services/order-service/src/main/resources/application-dev.yml`
- `backend/api-gateway/src/main/resources/application.yml`

## 3) UI livree

### Ecran principal Panier
- Header partenaire (nom/logo),
- Banniere partenaire ferme,
- Banniere minimum de commande non atteint,
- Liste items (Dismissible, quantite +/-),
- Edition customisation (bottom sheet: options + note cuisine),
- Carte resume commande (sous-total, livraison, service fee, remise, total),
- Champ promo + feedback utilisateur,
- CTA checkout desactive selon contraintes (panier vide, minimum, partenaire ferme, envoi en cours).

Fichiers:
- `lib/features/cart/presentation/cart_screen.dart`
- `lib/features/cart/presentation/widgets/cart_item_tile.dart`
- `lib/features/cart/presentation/widgets/order_summary_card.dart`
- `lib/features/cart/presentation/widgets/promo_code_field.dart`
- `lib/features/cart/presentation/widgets/partner_header.dart`
- `lib/features/cart/presentation/widgets/partner_closed_banner.dart`
- `lib/features/cart/presentation/widgets/minimum_order_banner.dart`

### FAB global panier
- FAB global ajoute au `MainScaffold`,
- badge anime sur nombre d'articles,
- ouverture route panier,
- trigger sync login + retry pending sync au resume app.

Fichiers:
- `lib/features/cart/presentation/cart_fab.dart`
- `lib/features/main/presentation/screens/main_scaffold.dart`

## 4) Integrations navigation et parcours achat
- Route ajoutee: `/cart`.
- Ecran `CartScreen` enregistre dans le router.
- Depuis `PartnerDetailsScreen`, le resultat de selection produit est converti en `CartItemModel` puis envoye au notifier.

Fichiers:
- `lib/config/routes/route_names.dart`
- `lib/config/routes/app_router.dart`
- `lib/features/partners/presentation/screens/partner_details_screen.dart`

## 5) Outils, libs et dependances

### Outils utilises
- Flutter / Dart
- Riverpod (`StateNotifierProvider`)
- Dio
- GoRouter
- CachedNetworkImage

## 6) Endpoints cibles utilises par le module
- `GET /api/cart`
- `PATCH /api/cart`
- `GET /api/partners/{partnerId}`
- `GET /api/partners/{partnerId}/delivery-fee`
- `POST /api/promo/validate` (avec fallback sur autres variantes)
- `POST /api/orders`

## 7) Duree du panier dans Redis
Question demandee: "combien de temps le panier reste en cache Redis ?"

Note importante:
- cette duree est definie uniquement en backend (pas dans le code Dart).

Constat sur ce repository (etat actuel analyse):
- endpoints `/api/cart` exposes via gateway vers order-service,
- panier stocke en Redis cote backend,
- TTL effectif borne entre 24h et 72h.

Conclusion:
- TTL minimum: 24h.
- TTL maximum: 72h (3 jours).

Impact cote app:
- la source de verite du panier est backend (Redis),
- le frontend garde un etat en memoire et synchronise via API,
- la persistance locale cart via Hive a ete retiree du flux principal.

## 8) Etapes de validation recommandees
1. Lancer l'app et ajouter des produits depuis un partenaire.
2. Verifier le badge FAB et l'ouverture ecran panier.
3. Fermer/reouvrir l'app puis se reconnecter: verifier que le panier est recharge via `GET /api/cart`.
4. Changer de partenaire: verifier le dialog de remplacement.
5. Modifier quantites/options: verifier recalcul total.
6. Tester promo et checkout.
7. Verifier logs reseau pour `GET/PATCH /api/cart` et `POST /api/orders`.

## 9) Configuration TTL Redis
- Variable: `CART_REDIS_TTL_HOURS`
- Valeur effective clamp:
  - minimum: 24
  - maximum: 72
- Valeur par defaut: 24
