@echo off
REM =============================================================================
REM SpeedLine Test Robot - One-Command Demo (Windows)
REM =============================================================================
REM Prerequisites: Docker Desktop running, Node.js 18+, Git
REM
REM Usage: run-demo.bat
REM =============================================================================

echo.
echo =====================================================
echo   SpeedLine - Robot de Tests Automatises
echo   Projet PFE - ISET Nabeul / DevWise
echo =====================================================
echo.

REM Get script directory
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

REM =============================================================================
REM Step 1: Check prerequisites
REM =============================================================================
echo [1/7] Checking prerequisites...

docker --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not installed. Install Docker Desktop first.
    pause
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running. Start Docker Desktop first.
    pause
    exit /b 1
)

node --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js is not installed. Install Node.js 18+ first.
    pause
    exit /b 1
)

echo   Docker and Node.js found.
echo.

REM =============================================================================
REM Step 2: Install dependencies
REM =============================================================================
echo [2/7] Installing test dependencies...
cd playwright
call npm ci --silent 2>nul || call npm install --silent
call npx playwright install chromium
cd ..\seed
call npm ci --silent 2>nul || call npm install --silent
cd ..
echo   Dependencies installed.
echo.

REM =============================================================================
REM Step 3: Start Docker environment
REM =============================================================================
echo [3/7] Starting Docker environment...
echo   This takes 2-5 minutes on first run.
echo.

docker compose -f docker-compose.test.yml down -v --remove-orphans 2>nul
docker compose -f docker-compose.test.yml up -d --build

echo.
echo   Waiting for services to start (60 seconds)...
timeout /t 60 /nobreak >nul

echo   Waiting for auth-service...
:WAIT_AUTH
curl -sf http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    timeout /t 5 /nobreak >nul
    goto WAIT_AUTH
)
echo   Gateway is UP!

REM Wait for other services
echo   Waiting 30 more seconds for all services...
timeout /t 30 /nobreak >nul

echo.
docker ps --format "    {{.Names}}: {{.Status}}" | findstr sl-test
echo.

REM =============================================================================
REM Step 4: Seed test data
REM =============================================================================
echo [4/7] Seeding test database...
cd seed
set DB_HOST=localhost
set DB_PORT=5433
set DB_USER=postgres
set DB_PASSWORD=postgres123
set AUTH_DB_NAME=speedline_auth
call node seed-test-data.js
cd ..
echo.

REM =============================================================================
REM Step 5: Run tests
REM =============================================================================
echo [5/7] Running API tests (Playwright)...
cd playwright
del /q allure-results\* 2>nul
call npx playwright test --project=api-tests --reporter=list,allure-playwright
echo.

REM =============================================================================
REM Step 6: Performance test
REM =============================================================================
echo [6/7] Performance test...
k6 version >nul 2>&1
if errorlevel 1 (
    echo   k6 not installed. Skipping. Install with: choco install k6
) else (
    k6 run --quiet --duration 10s --vus 5 ..\performance\k6-load-test.js
)
echo.

REM =============================================================================
REM Step 7: Report
REM =============================================================================
echo [7/7] Generating report...
allure version >nul 2>&1
if errorlevel 1 (
    echo   Allure not installed. Opening Playwright report...
    call npx playwright show-report
) else (
    allure generate allure-results --clean -o allure-report
    echo   Opening Allure report...
    start allure open allure-report --port 9090
)

echo.
echo =====================================================
echo   DEMO COMPLETE
echo =====================================================
echo.
echo   Report: http://localhost:9090
echo   Stop:   docker compose -f docker-compose.test.yml down
echo   Re-run: cd playwright ^&^& npm test
echo.
pause
