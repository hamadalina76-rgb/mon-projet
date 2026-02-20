# Script PowerShell pour initialiser les topics et subscriptions Pub/Sub dans l'émulateur

Write-Host "Initializing Pub/Sub Emulator topics and subscriptions..." -ForegroundColor Cyan

# Variables
$PUBSUB_EMULATOR_HOST = "localhost:8085"
$PROJECT_ID = "speedline-local"

# Set environment variable for gcloud
$env:PUBSUB_EMULATOR_HOST = $PUBSUB_EMULATOR_HOST

# Create partner-events topic
Write-Host "`nCreating topic: partner-events" -ForegroundColor Yellow
gcloud pubsub topics create partner-events --project=$PROJECT_ID 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Topic 'partner-events' created successfully" -ForegroundColor Green
} else {
    Write-Host "✓ Topic 'partner-events' already exists or created" -ForegroundColor Green
}

# Create subscription for notification service
Write-Host "`nCreating subscription: partner-events-notification-sub" -ForegroundColor Yellow
gcloud pubsub subscriptions create partner-events-notification-sub `
    --topic=partner-events `
    --project=$PROJECT_ID 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Subscription 'partner-events-notification-sub' created successfully" -ForegroundColor Green
} else {
    Write-Host "✓ Subscription 'partner-events-notification-sub' already exists or created" -ForegroundColor Green
}

Write-Host "`n✅ Pub/Sub Emulator initialization completed!" -ForegroundColor Green
Write-Host "`nEmulator host: $PUBSUB_EMULATOR_HOST" -ForegroundColor Cyan
Write-Host "Project ID: $PROJECT_ID" -ForegroundColor Cyan
