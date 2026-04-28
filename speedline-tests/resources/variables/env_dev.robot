*** Variables ***
# Development Environment Configuration
${ENV_NAME}                     Development
${API_TIMEOUT}                  30s
${UI_TIMEOUT}                   40s

# Admin Panel - Development (Local)
${ADMIN_URL_DEV}                http://localhost:3000
${ADMIN_LOGIN_URL_DEV}          ${ADMIN_URL_DEV}/auth/login

# Credentials - Development
${DEV_ADMIN_EMAIL}              admin@dev.local
${DEV_ADMIN_PASSWORD}           DevPassword123

# Expected Page Elements
${DASHBOARD_VISIBLE}            True
${SIDEBAR_VISIBLE}              True
${ADMIN_FUNCTIONS_VISIBLE}      True

# Development-specific settings
${HEADLESS}                     False
${BROWSER_LOGS}                 True
${NETWORK_THROTTLE}             True
