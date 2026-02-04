# ============================================
# Script d'Arrêt - SpeedLine Backend
# ============================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Arrêt des Services SpeedLine" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Arrêter les processus Java (microservices Spring Boot)
Write-Host "Arrêt des microservices Spring Boot..." -ForegroundColor Yellow

$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*speedline*" -or $_.CommandLine -like "*spring-boot*"
}

if ($javaProcesses) {
    Write-Host "  Trouvé $($javaProcesses.Count) processus Java à arrêter..." -ForegroundColor Yellow
    $javaProcesses | ForEach-Object {
        Write-Host "    → Arrêt du processus PID $($_.Id)..." -ForegroundColor Gray
        Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  ✓ Microservices arrêtés" -ForegroundColor Green
} else {
    Write-Host "  ✓ Aucun microservice en cours d'exécution" -ForegroundColor Green
}

Write-Host ""

# Option pour arrêter les services Docker
Write-Host "Voulez-vous arrêter les services Docker (PostgreSQL, MongoDB, etc.) ?" -ForegroundColor Yellow
$response = Read-Host "  (O/N)"

if ($response -eq "O" -or $response -eq "o") {
    Write-Host ""
    Write-Host "Arrêt des services Docker..." -ForegroundColor Yellow
    docker-compose down
    Write-Host "  ✓ Services Docker arrêtés" -ForegroundColor Green
} else {
    Write-Host "  → Services Docker conservés en cours d'exécution" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Arrêt terminé" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
