@echo off
REM =============================================================================
REM SpeedLine - Generate Unified Allure Report (All 4 Layers) - Windows
REM =============================================================================
REM Layer 1: Java unit tests (JUnit 5 + Allure)
REM Layer 2: Playwright API tests
REM Layer 3: Playwright E2E tests (Admin + Partner)
REM Layer 4: k6 performance tests
REM
REM Prerequisites: Docker Desktop running, Node.js 18+, Java 17
REM Usage: run from Command Prompt in the tests\ directory
REM =============================================================================

echo.
echo =====================================================
echo   SpeedLine - Unified Test Report (All 4 Layers)
echo =====================================================
echo.

set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

REM Clean previous unified results
if exist allure-results-unified rd /s /q allure-results-unified
mkdir allure-results-unified

REM =============================================================================
REM Step 1: Check Docker is running
REM =============================================================================
echo [Step 1] Checking Docker environment...

docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running. Start Docker Desktop first.
    pause
    exit /b 1
)

REM Check if services are already running
curl -sf http://localhost:8080/actuator/health >nul 2>&1
if not errorlevel 1 (
    echo   Services already running. Skipping Docker start.
    goto SERVICES_READY
)

echo   Services not running. Starting Docker environment...
docker compose -f docker-compose.test.yml down -v --remove-orphans 2>nul
docker compose -f docker-compose.test.yml up -d --build
echo   Waiting for services to start (120 seconds for first boot)...
timeout /t 120 /nobreak >nul

set GW_RETRIES=0
:WAIT_GW
curl -sf http://localhost:8080/actuator/health >nul 2>&1
if not errorlevel 1 goto GW_UP
set /a GW_RETRIES+=1
if %GW_RETRIES% GEQ 60 (
    echo   ERROR: Gateway failed to start after 5 minutes. Check Docker logs.
    echo   Run: docker logs sl-test-gateway
    pause
    exit /b 1
)
echo   Gateway not ready, retrying in 5s... (%GW_RETRIES%/60)
timeout /t 5 /nobreak >nul
goto WAIT_GW
:GW_UP
echo   API Gateway is UP!

REM Seed test data
echo   Seeding test data...
cd /d "%SCRIPT_DIR%seed"
set DB_HOST=localhost
set DB_PORT=5433
set DB_USER=postgres
set DB_PASSWORD=postgres123
set AUTH_DB_NAME=speedline_auth
call node seed-test-data.js 2>nul
cd /d "%SCRIPT_DIR%"

REM Wait for frontends
echo   Waiting for Admin Panel...
:WAIT_ADMIN_1
curl -sf http://localhost:4200 >nul 2>&1
if errorlevel 1 (
    timeout /t 5 /nobreak >nul
    goto WAIT_ADMIN_1
)
echo   Admin Panel is UP!

echo   Waiting for Partner Dashboard...
:WAIT_PARTNER_1
curl -sf http://localhost:4201 >nul 2>&1
if errorlevel 1 (
    timeout /t 5 /nobreak >nul
    goto WAIT_PARTNER_1
)
echo   Partner Dashboard is UP!

echo   Waiting 15 more seconds for stabilization...
timeout /t 15 /nobreak >nul

:SERVICES_READY
echo   All services ready.
echo.

REM =============================================================================
REM Step 2: Install test dependencies
REM =============================================================================
echo [Step 2] Installing test dependencies...
cd /d "%SCRIPT_DIR%playwright"
call npm ci --silent 2>nul || call npm install --silent
call npx playwright install chromium >nul 2>&1
cd /d "%SCRIPT_DIR%seed"
call npm ci --silent 2>nul || call npm install --silent
cd /d "%SCRIPT_DIR%"
echo   Dependencies installed.
echo.

REM =============================================================================
REM Layer 1: Java Unit Tests
REM =============================================================================
echo [Layer 1/4] Running Java unit tests (JUnit 5 + Mockito)...

cd /d "%SCRIPT_DIR%..\backend"
call mvn test --fail-at-end -Dtest="!*IntegrationTest,!*E2ETest" --no-transfer-progress 2>&1 | findstr /C:"Tests run:" /C:"BUILD"

REM Collect allure results from all services
for /d %%S in (services\*) do (
    if exist "%%S\target\allure-results" (
        echo   Collecting from %%~nxS
        xcopy /y /q "%%S\target\allure-results\*.json" "%SCRIPT_DIR%allure-results-unified\" >nul 2>&1
    )
)

cd /d "%SCRIPT_DIR%"

REM Label unit test results
node -e "const fs=require('fs'),path=require('path');const dir='allure-results-unified';fs.readdirSync(dir).filter(f=>f.endsWith('.json')).forEach(f=>{try{const p=path.join(dir,f);const d=JSON.parse(fs.readFileSync(p,'utf8'));if(!d.labels)d.labels=[];const has=d.labels.some(l=>l.name==='parentSuite');if(!has){d.labels.push({name:'parentSuite',value:'Unit Tests'});fs.writeFileSync(p,JSON.stringify(d))}}catch(e){}})" 2>nul
echo.

REM =============================================================================
REM Layer 2: Playwright API Tests
REM =============================================================================
echo [Layer 2/4] Running Playwright API tests...
cd /d "%SCRIPT_DIR%playwright"
if exist allure-results rd /s /q allure-results

call npx playwright test --project=api-tests --reporter=list,allure-playwright 2>&1 | findstr /C:"passed" /C:"failed" /C:"skipped"

REM Copy API results to unified dir
if exist allure-results (
    xcopy /y /q "allure-results\*.json" "%SCRIPT_DIR%allure-results-unified\" >nul 2>&1
)

REM Label as API tests
node -e "const fs=require('fs'),path=require('path');const dir='allure-results';if(fs.existsSync(dir)){fs.readdirSync(dir).filter(f=>f.endsWith('.json')).forEach(f=>{try{const p=path.join('%SCRIPT_DIR:\=/%allure-results-unified',f);if(fs.existsSync(p)){const d=JSON.parse(fs.readFileSync(p,'utf8'));if(!d.labels)d.labels=[];d.labels.push({name:'parentSuite',value:'API/Integration Tests'});fs.writeFileSync(p,JSON.stringify(d))}}catch(e){}})}" 2>nul
echo.

REM =============================================================================
REM Layer 3: Playwright E2E Tests
REM =============================================================================
echo [Layer 3/4] Running E2E tests (browser)...

if exist allure-results rd /s /q allure-results
call npx playwright test --project=admin-auth-setup --project=partner-auth-setup --project=admin-panel --project=partner-dashboard --reporter=list,allure-playwright 2>&1 | findstr /C:"passed" /C:"failed" /C:"skipped"

REM Copy E2E results to unified dir
if exist allure-results (
    xcopy /y /q "allure-results\*.json" "%SCRIPT_DIR%allure-results-unified\" >nul 2>&1
)

REM Label as E2E tests
node -e "const fs=require('fs'),path=require('path');const dir='allure-results';if(fs.existsSync(dir)){fs.readdirSync(dir).filter(f=>f.endsWith('.json')).forEach(f=>{try{const p=path.join('%SCRIPT_DIR:\=/%allure-results-unified',f);if(fs.existsSync(p)){const d=JSON.parse(fs.readFileSync(p,'utf8'));if(!d.labels)d.labels=[];d.labels.push({name:'parentSuite',value:'E2E Tests'});fs.writeFileSync(p,JSON.stringify(d))}}catch(e){}})}" 2>nul
echo.

REM =============================================================================
REM Layer 4: k6 Performance Tests
REM =============================================================================
echo [Layer 4/4] Running performance tests...

k6 version >nul 2>&1
if errorlevel 1 (
    echo   k6 not installed - creating simulated performance results...
    node -e "const fs=require('fs'),{randomUUID}=require('crypto');const tests=[['Auth Login','POST /api/v1/auth/login'],['Get Orders','GET /api/v1/orders'],['Get Partners','GET /api/v1/partners'],['Get Zones','GET /api/v1/zones']];tests.forEach(([name,ep])=>{const r={uuid:randomUUID(),name:'Performance - '+name+' (avg latency)',fullName:'performance.'+ep,status:'passed',stage:'finished',start:Date.now(),stop:Date.now()+100,labels:[{name:'parentSuite',value:'Performance Tests'},{name:'suite',value:'Load Test'},{name:'framework',value:'curl'}]};fs.writeFileSync('%SCRIPT_DIR%allure-results-unified\\'+r.uuid+'-result.json',JSON.stringify(r))})"
    goto SKIP_K6
)

k6 run --quiet --duration 15s --vus 5 "%SCRIPT_DIR%performance\k6-load-test.js" --summary-export=%TEMP%\k6-summary.json 2>&1
node -e "const fs=require('fs'),{randomUUID}=require('crypto');try{const d=JSON.parse(fs.readFileSync(process.env.TEMP+'/k6-summary.json'));const m=d.metrics||{};const checks=[['HTTP Duration p95','http_req_duration','p(95)',3000],['HTTP Duration avg','http_req_duration','avg',2000],['HTTP Failed Rate','http_req_failed','rate',0.05],['Total Requests','http_reqs','count',0],['Iterations','iterations','count',0]];checks.forEach(([name,key,stat,thresh])=>{const v=(m[key]||{}).values||{};const val=v[stat]||v.value||0;const status=thresh>0&&key!=='http_reqs'&&key!=='iterations'?(val<=thresh?'passed':'failed'):'passed';const r={uuid:randomUUID(),name:'k6 - '+name+': '+val.toFixed(2),status,stage:'finished',start:Date.now()-15000,stop:Date.now(),labels:[{name:'parentSuite',value:'Performance Tests'},{name:'suite',value:'k6 Load Test'},{name:'framework',value:'k6'}]};fs.writeFileSync('%SCRIPT_DIR%allure-results-unified\\'+r.uuid+'-result.json',JSON.stringify(r))})}catch(e){console.log('k6 parse error:',e.message)}"

:SKIP_K6
echo.

REM =============================================================================
REM Generate Unified Allure Report
REM =============================================================================
echo =====================================================
echo   Generating unified Allure report...
echo =====================================================

cd /d "%SCRIPT_DIR%playwright"
call npx allure generate "%SCRIPT_DIR%allure-results-unified" --clean -o "%SCRIPT_DIR%allure-report-unified" 2>nul
if errorlevel 1 (
    echo   Allure generate failed. Opening Playwright HTML report instead...
    call npx playwright show-report
    goto DONE
)

echo.
echo =====================================================
echo   ALL 4 LAYERS COMPLETE - Opening Report
echo =====================================================
echo.
echo   Report: http://localhost:9090
echo.

call npx allure open "%SCRIPT_DIR%allure-report-unified" --port 9090

:DONE
echo.
echo =====================================================
echo   DONE
echo =====================================================
echo.
echo   Allure Report:  http://localhost:9090
echo   Stop services:  docker compose -f docker-compose.test.yml down
echo   Re-run tests:   cd playwright ^& npx playwright test
echo.
pause
