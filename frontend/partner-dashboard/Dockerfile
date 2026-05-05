FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN mkdir -p src/environments && printf 'export const environment = {\n  production: false,\n  apiUrl: "http://localhost:8080/api/v1",\n  wsUrl: "ws://localhost:8080",\n  mapboxToken: "pk.test-placeholder",\n  defaultLanguage: "fr",\n  supportedLanguages: ["fr", "en", "ar"],\n  uploadMaxSize: 5242880,\n};\n' > src/environments/environment.ts
RUN npm run build -- --configuration local

FROM caddy:2-alpine
WORKDIR /usr/share/caddy
COPY Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist/partner-dashboard/browser .
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN sed -i 's/\r$//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
