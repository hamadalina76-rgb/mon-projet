FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN echo "export const environment = { \
  production: false, \
  apiUrl: 'http://localhost:8080/api/v1', \
  apiBaseUrl: 'http://localhost:8080', \
  uploadsBaseUrl: 'http://localhost:8080', \
  notificationsApiUrl: 'http://localhost:8080/api', \
  wsUrl: 'ws://localhost:8080', \
  mapboxToken: 'pk.test-placeholder', \
  defaultLanguage: 'fr', \
  supportedLanguages: ['fr', 'en', 'ar'] \
};" > src/environments/environment.ts
RUN npm run build -- --configuration local

FROM caddy:2-alpine
WORKDIR /usr/share/caddy
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist/admin-panel/browser .
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
