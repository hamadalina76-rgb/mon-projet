# Nearby Partner - Documentation Complete

## Objectif
L'endpoint GET /partners/nearby permet de retourner une page de partenaires proches d'une position GPS, avec des filtres combinables (AND), un tri dynamique et une distance calculee en base via PostGIS.

## Endpoint HTTP
- Methode: GET
- URL: /partners/nearby
- Reponse: Page<PartnerDTO>
- HTTP 200 avec content vide quand aucun resultat

## Parametres supportes
Parametres obligatoires:
- lat (BigDecimal)
- lng (BigDecimal)

Parametres optionnels:
- page (defaut 0)
- size (defaut 20, max 100)
- isOpen (Boolean)
- categoryId (String)
- minRating (Double)
- maxDeliveryTime (Integer)
- freeDelivery (Boolean)
- sortBy (distance | rating | deliveryTime | popularity, defaut distance)

## Contrat et flow implementes
Flow final:
1. Controller recoit les query params.
2. Controller construit PartnerFilterRequest.
3. Service getNearbyPartners(PartnerFilterRequest) applique les regles metier.
4. Repository execute les requetes natives PostGIS (count + data).
5. Service mappe les resultats en PartnerDTO avec distanceKm.
6. Retour pagine PageImpl.

## Comportement des filtres
Tous les filtres sont combinables en logique AND:
- isOpen: filtre ouvert/ferme base sur l'heure courante et les horaires.
- categoryId: filtre sur category_ids CSV avec support parent/enfants.
- minRating: filtre sur note minimale.
- maxDeliveryTime: filtre sur preparation_time max.
- freeDelivery=true: filtre delivery_fee = 0.

Regle isOpen actuelle:
- isOpen=true: accepts_orders=true ET un slot opening_hours_json du jour couvre l'heure courante.
- isOpen=false: partenaire ferme maintenant (accepts_orders=false OU pas de slot valide maintenant).
- Gestion incluse des cas overnight (openTime > closeTime), is24Hours et isClosed.

Utilite des colonnes/champs:
- opening_hours_json (colonne DB): source de verite des horaires exploites par le filtre isOpen.
- opening_hours (colonne legacy V1): historique/compatibilite, migree vers opening_hours_json en V2.
- isCurrentlyOpen (champ Java transient dans [Partner.java](src/main/java/com/speedline/partner/domain/Partner.java)): non persiste en base, utile pour exposition/calcul applicatif, mais pas une colonne DB.


Utilite de `location`:
- Permet les calculs geospatiaux PostGIS performants: `ST_DWithin`, `ST_Distance`, tri par distance.
- `latitude` / `longitude` sont conservees pour compatibilite/metier, mais `location` est la representation spatiale optimisee pour les requetes Nearby.

Cas categoryId parent:
- Si categoryId correspond a une categorie parent, les descendants sont inclus.
- Le service construit un regex tokenise PostgreSQL de type (^|,)(id1|id2|id3)(,|$).
- Le filtre SQL utilise ce regex sur category_ids.

## Tri supporte
Tri dynamique SQL:
- distance: distance croissante
- rating: note decroissante
- deliveryTime: preparation_time croissant
- popularity: total_orders decroissant

Fallback:
- Toute valeur sortBy invalide retombe sur distance.

## PostGIS et distance
La distance est calculee en SQL, pas en Java:
- ST_DWithin(..., radiusMeters) pour limiter au rayon
- ST_Distance(...)/1000.0 pour distance_km

Le champ spatial Partner.location est maintenu via hooks JPA:
- @PrePersist
- @PreUpdate

Ces hooks synchronisent location depuis latitude/longitude.

## Rayon et cache Redis
Rayon utilise:
- Lecture de config:nearby:radius_km depuis Redis
- Fallback sur la configuration application si la cle Redis est absente

Cache utilise:
- Cle cache composee avec lat/lng/page/size + filtres + tri
- Reponse paginee mise en cache en JSON

## Fichiers modifies pour Nearby Partner
- src/main/java/com/speedline/partner/dto/request/PartnerFilterRequest.java
- src/main/java/com/speedline/partner/controller/PartnerApi.java
- src/main/java/com/speedline/partner/controller/PartnerController.java
- src/main/java/com/speedline/partner/service/PartnerService.java
- src/main/java/com/speedline/partner/service/impl/PartnerServiceImpl.java
- src/main/java/com/speedline/partner/repository/PartnerRepository.java
- src/main/java/com/speedline/partner/domain/Partner.java
- src/test/java/com/speedline/partner/controller/PartnerControllerNearbyTest.java

## Swagger
Swagger UI Partner Service:
- http://localhost:8083/swagger-ui/index.html

OpenAPI JSON:
- http://localhost:8083/v3/api-docs

## Tester les filtres avec Swagger (pas a pas)
1. Demarrer partner-service puis ouvrir Swagger UI:
   - http://localhost:8083/swagger-ui/index.html
2. Ouvrir la section Partner puis l'endpoint GET /partners/nearby.
3. Cliquer sur Try it out.
4. Saisir au minimum:
   - lat = 36.8065
   - lng = 10.1815
5. Tester les filtres un par un puis en combinaison.

Jeux de test conseilles:

- Base (sans filtre):
  - lat=36.8065, lng=10.1815, page=0, size=20, sortBy=distance

- Ouverts uniquement:
  - isOpen=true

- Categorie (parent + enfants):
  - categoryId=12

- Note minimale:
  - minRating=4.0

- Temps max:
  - maxDeliveryTime=30

- Livraison gratuite:
  - freeDelivery=true

- Tri par note:
  - sortBy=rating

- Tri par popularite:
  - sortBy=popularity

- Filtres combines (AND):
  - isOpen=true, freeDelivery=true, minRating=4.0, sortBy=distance

Resultats attendus:
- 200 OK avec un objet page.
- content peut etre vide ([]) si aucun partenaire ne matche.
- Les filtres se cumulent en AND.

## Commandes de test Redis (Nearby)
Depuis la racine backend avec Docker demarre:

- Verifier que Redis repond:
  docker exec -it speedline-redis redis-cli PING

- Lire le rayon configure:
  docker exec -it speedline-redis redis-cli GET config:nearby:radius_km

- Definir un rayon de test (ex: 8 km):
  docker exec -it speedline-redis redis-cli SET config:nearby:radius_km 8

- Lister les cles de cache Nearby:
  docker exec -it speedline-redis redis-cli KEYS "partners:nearby:*"

- Purger les cles cache Nearby (PowerShell):
  docker exec -it speedline-redis redis-cli --scan --pattern "partners:nearby:*"

Remarque:
- Le nom du container peut varier selon votre environnement Docker Compose.
- Adaptez speedline-redis si besoin avec docker ps.

## Commandes de test PartnerControllerNearbyTest
Depuis backend/services/partner-service:

- Lancer la classe de test:
  mvn -Dtest=PartnerControllerNearbyTest test

- Lancer un seul test de la classe:
  mvn -Dtest=PartnerControllerNearbyTest#tc21_intersection_shouldForwardAllCombinedFilters test

- Avec details:
  mvn -Dtest=PartnerControllerNearbyTest test -X

## Resultat attendu principal
- L'API /partners/nearby respecte pagination + filtres + tri.
- categoryId parent inclut ses categories enfants.
- Distance et tri sont calcules en base PostGIS.
- Le endpoint retourne 200 avec content vide si aucun resultat.
