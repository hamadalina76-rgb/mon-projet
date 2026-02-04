# ⚠️ MongoDB - Pas de Migrations Flyway

Ce service utilise **MongoDB** (base NoSQL), pas PostgreSQL.

**MongoDB ne nécessite PAS de migrations Flyway !**

## Comment ça fonctionne ?

1. **Collections créées automatiquement** lors du premier insert
2. **Index créés automatiquement** via `@Indexed` dans les entités Java
3. **Configuration** dans `application.yml` avec `auto-index-creation: true`

## Collections MongoDB

- `reviews` - Avis clients
- `ratings` - Notes agrégées (partenaires, livreurs)

## Index

Les index sont définis dans les entités Java avec `@Indexed` :
- `Review.orderId` → Index simple
- `Review.customerId` → Index simple
- `Review.targetType` → Index simple
- `Review.targetId` → Index simple
- `Rating.entityKey` → Index unique
- `Rating.entityType` → Index simple
- `Rating.entityId` → Index simple

---

**Voir `MONGODB_CONFIGURATION_COMPLETE.md` pour plus de détails.**
