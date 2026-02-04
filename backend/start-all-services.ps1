# ============================================
# Script de Démarrage - SpeedLine Backend
# ============================================
# Ce script démarre tous les microservices dans l'ordre correct

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Démarrage des Services SpeedLine" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier que Docker est démarré
Write-Host "Vérification de Docker..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "✓ Docker est démarré" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker n'est pas démarré. Veuillez démarrer Docker Desktop." -ForegroundColor Red
    exit 1
}

# Vérifier que les services d'infrastructure sont démarrés
Write-Host ""
Write-Host "Vérification des services d'infrastructure..." -ForegroundColor Yellow
$services = docker-compose ps --services --filter "status=running"
if ($services -notcontains "postgres" -or $services -notcontains "eureka-server") {
    Write-Host "⚠ Les services d'infrastructure ne sont pas tous démarrés." -ForegroundColor Yellow
    Write-Host "  Démarrage des services d'infrastructure..." -ForegroundColor Yellow
    docker-compose up -d postgres mongodb redis clickhouse kafka zookeeper kafka-ui eureka-server
    Write-Host "  Attente de 10 secondes pour le démarrage..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    Write-Host "✓ Services d'infrastructure démarrés" -ForegroundColor Green
} else {
    Write-Host "✓ Services d'infrastructure déjà démarrés" -ForegroundColor Green
}

Write-Host ""
Write-Host "Démarrage des microservices..." -ForegroundColor Yellow
Write-Host ""

# Fonction pour démarrer un service
function Start-Service {
    param(
        [string]$ServiceName,
        [int]$Port,
        [int]$DelaySeconds = 15
    )
    
    Write-Host "  → Démarrage de $ServiceName (port $Port)..." -ForegroundColor Cyan
    
    # Vérifier si le port est déjà utilisé
    $portInUse = netstat -ano | findstr ":$Port" | findstr "LISTENING"
    if ($portInUse) {
        Write-Host "    ⚠ Port $Port déjà utilisé. Service peut-être déjà démarré." -ForegroundColor Yellow
    } else {
        # Démarrer le service dans une nouvelle fenêtre PowerShell
        $scriptPath = Join-Path $PSScriptRoot "services\$ServiceName"
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$scriptPath'; Write-Host '=== $ServiceName (Port $Port) ===' -ForegroundColor Green; mvn spring-boot:run"
        
        # Attendre avant de démarrer le service suivant
        Write-Host "    ⏳ Attente de $DelaySeconds secondes..." -ForegroundColor Gray
        Start-Sleep -Seconds $DelaySeconds
        Write-Host "    ✓ $ServiceName démarré" -ForegroundColor Green
    }
    Write-Host ""
}

# Démarrer les services dans l'ordre
Write-Host "Ordre de démarrage :" -ForegroundColor Magenta
Write-Host "  1. Auth Service (base pour l'authentification)" -ForegroundColor Gray
Write-Host "  2. User Service" -ForegroundColor Gray
Write-Host "  3. Partner Service" -ForegroundColor Gray
Write-Host "  4. Location Service" -ForegroundColor Gray
Write-Host "  5. Payment Service" -ForegroundColor Gray
Write-Host "  6. Promotion Service" -ForegroundColor Gray
Write-Host "  7. Order Service" -ForegroundColor Gray
Write-Host "  8. Delivery Service" -ForegroundColor Gray
Write-Host "  9. Notification Service" -ForegroundColor Gray
Write-Host "  10. Review Service" -ForegroundColor Gray
Write-Host "  11. Support Service" -ForegroundColor Gray
Write-Host "  12. Analytics Service" -ForegroundColor Gray
Write-Host "  13. API Gateway (en dernier)" -ForegroundColor Gray
Write-Host ""

# Démarrer les services
Start-Service "auth-service" 8081 20
Start-Service "user-service" 8082 15
Start-Service "partner-service" 8083 15
Start-Service "location-service" 8088 15
Start-Service "payment-service" 8086 15
Start-Service "promotion-service" 8092 15
Start-Service "order-service" 8084 15
Start-Service "delivery-service" 8085 15
Start-Service "notification-service" 8087 15
Start-Service "review-service" 8091 15
Start-Service "support-service" 8093 15
Start-Service "analytics-service" 8089 15
Start-Service "api-gateway" 8080 15

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Tous les services sont en cours de démarrage" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Vérifications :" -ForegroundColor Yellow
Write-Host "  • Vérifiez les logs dans chaque fenêtre PowerShell" -ForegroundColor Gray
Write-Host "  • Eureka Dashboard : http://localhost:8761" -ForegroundColor Gray
Write-Host "  • Kafka UI : http://localhost:8090" -ForegroundColor Gray
Write-Host ""
Write-Host "Pour arrêter tous les services, fermez toutes les fenêtres PowerShell." -ForegroundColor Yellow
Write-Host ""
