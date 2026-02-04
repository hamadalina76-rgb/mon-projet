# ⚠️ MongoDB - Pas de Migrations Flyway

Ce service utilise **MongoDB** (base NoSQL), pas PostgreSQL.

**MongoDB ne nécessite PAS de migrations Flyway !**

## Comment ça fonctionne ?

1. **Collections créées automatiquement** lors du premier insert
2. **Index créés automatiquement** via `@Indexed` dans les entités Java
3. **Configuration** dans `application.yml` avec `auto-index-creation: true`

## Collections MongoDB

- `notifications` - Notifications utilisateurs
- `push_tokens` - Tokens FCM pour notifications push

## Index

Les index sont définis dans les entités Java avec `@Indexed` :
- `Notification.userId` → Index simple
- `PushToken.userId` → Index simple
- `PushToken.token` → Index unique

---

**Voir `MONGODB_CONFIGURATION_COMPLETE.md` pour plus de détails.**
