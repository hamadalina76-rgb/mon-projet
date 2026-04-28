*** Variables ***
# Production Environment Configuration
${ENV_NAME}                     Production
${API_TIMEOUT}                  15s
${UI_TIMEOUT}                   20s

# Admin Panel - Production
${ADMIN_URL_PROD}               https://admin-panel-392205979525.europe-west1.run.app
${ADMIN_LOGIN_URL_PROD}         ${ADMIN_URL_PROD}/auth/login?returnUrl=%2Fdashboard

# Credentials - Production (Superadmin)
${PROD_ADMIN_EMAIL}             superadmin@speedline.com
${PROD_ADMIN_PASSWORD}          Devwise#159

# Expected Page Elements
${DASHBOARD_VISIBLE}            True
${SIDEBAR_VISIBLE}              True
${ADMIN_FUNCTIONS_VISIBLE}      True
