$ErrorActionPreference='Stop'
function ConvertTo-Base64Url([string]$text) {
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
  $b64 = [System.Convert]::ToBase64String($bytes)
  return $b64.TrimEnd('=') -replace '\+','-' -replace '/','_'
}

$headerJson = '{"alg":"HS256","typ":"JWT"}'
$exp = [DateTimeOffset]::UtcNow.AddMinutes(20).ToUnixTimeSeconds()
$payloadObj = @{
  sub = '88'
  userId = '88'
  role = 'ROLE_COURIER'
  exp = $exp
}
$payloadJson = $payloadObj | ConvertTo-Json -Compress
$jwt = "$(ConvertTo-Base64Url $headerJson).$(ConvertTo-Base64Url $payloadJson).signature"
Write-Host "JWT generated courierId=88 exp=$exp"

$wsUri = [Uri]::new("ws://127.0.0.1:8080/ws/location?token=$jwt")
$ws = [System.Net.WebSockets.ClientWebSocket]::new()
$cts = [System.Threading.CancellationTokenSource]::new()
$cts.CancelAfter([TimeSpan]::FromSeconds(12))
$ws.ConnectAsync($wsUri, $cts.Token).GetAwaiter().GetResult()

$positionPayload = @{ 
  type = 'POSITION_UPDATE';
  payload = @{ 
    lat = 36.8423;
    lng = 10.1937;
    accuracy = 5.0;
    speed = 12.3;
    heading = 180.0;
    batteryLevel = 87;
    timestamp = [DateTime]::UtcNow.ToString('o')
  }
} | ConvertTo-Json -Depth 6 -Compress

$bytes = [System.Text.Encoding]::UTF8.GetBytes($positionPayload)
$segment = [ArraySegment[byte]]::new($bytes)
$ws.SendAsync($segment, [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $cts.Token).GetAwaiter().GetResult()
Start-Sleep -Milliseconds 700
$ws.CloseAsync([System.Net.WebSockets.WebSocketCloseStatus]::NormalClosure, 'done', $cts.Token).GetAwaiter().GetResult()
$ws.Dispose()
Write-Host 'WS position sent.'

try {
  $resNoAuth = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/v1/tracking/couriers/88' -Method GET -TimeoutSec 10
  Write-Host 'REST no-auth status:' $resNoAuth.StatusCode
  Write-Host $resNoAuth.Content
} catch {
  Write-Host 'REST no-auth failed:' $_.Exception.Message
}

try {
  $headers = @{ Authorization = "Bearer $jwt" }
  $resAuth = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/v1/tracking/couriers/88' -Method GET -Headers $headers -TimeoutSec 10
  Write-Host 'REST with-auth status:' $resAuth.StatusCode
  Write-Host $resAuth.Content
} catch {
  Write-Host 'REST with-auth failed:' $_.Exception.Message
}
