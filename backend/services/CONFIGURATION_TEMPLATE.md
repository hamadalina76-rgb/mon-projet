# ============================================================================
# TEMPLATE - CONFIGURATION POUR TOUS LES MICROSERVICES
# ============================================================================
# Ce template peut être copié et adapté pour chaque service
# Remplacer [SERVICE_NAME] par le nom du service
# Remplacer [SERVICE_PORT] par le port du service

# ============================================================================
# .env.dev TEMPLATE
# ============================================================================
SPRING_PROFILES_ACTIVE=dev
SPRING_APPLICATION_NAME=[SERVICE_NAME]

EUREKA_USER=eureka
EUREKA_PASSWORD=eureka123
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:eureka123@localhost:8761/eureka/

SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/[SERVICE_NAME]_dev?useSSL=false&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root

SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

SERVER_PORT=[SERVICE_PORT]

LOG_LEVEL=DEBUG

# ============================================================================
# .env.staging TEMPLATE
# ============================================================================
SPRING_PROFILES_ACTIVE=staging
EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:eureka123@eureka:8761/eureka/
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/[SERVICE_NAME]_staging
SPRING_REDIS_HOST=redis

LOG_LEVEL=INFO

# ============================================================================
# .env.prod TEMPLATE
# ============================================================================
SPRING_PROFILES_ACTIVE=prod
EUREKA_USER=${EUREKA_USER_PROD}
EUREKA_PASSWORD=${EUREKA_PASSWORD_PROD}
SPRING_DATASOURCE_URL=${DB_URL_PROD}
SPRING_DATASOURCE_USERNAME=${DB_USERNAME_PROD}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD_PROD}
SPRING_REDIS_HOST=${REDIS_HOST_PROD}
SPRING_REDIS_PASSWORD=${REDIS_PASSWORD_PROD}

LOG_LEVEL=ERROR

# ============================================================================
# application-dev.yml TEMPLATE
# ============================================================================
server:
  port: ${SERVER_PORT:[SERVICE_PORT]}

spring:
  application:
    name: [SERVICE_NAME]
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
  redis:
    host: ${SPRING_REDIS_HOST}

eureka:
  client:
    service-url:
      defaultZone: http://${EUREKA_USER}:${EUREKA_PASSWORD}@localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    org.springframework: DEBUG
    com.speedline: DEBUG

