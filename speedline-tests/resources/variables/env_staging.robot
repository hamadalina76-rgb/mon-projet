*** Variables ***
# Staging Environment Configuration
${ENV_NAME}                     Staging
${API_TIMEOUT}                  15s
${UI_TIMEOUT}                   20s

# Admin Panel - Staging
${ADMIN_URL_STAGING}            https://admin-staging.speedline.com
${ADMIN_LOGIN_URL_STAGING}      ${ADMIN_URL_STAGING}/auth/login

# Credentials - Staging (Update wenn verfügbar)
${STAGING_ADMIN_EMAIL}          admin@staging.speedline.com
${STAGING_ADMIN_PASSWORD}       StagingPassword123

# Expected Page Elements
${DASHBOARD_VISIBLE}            True
${SIDEBAR_VISIBLE}              True
${ADMIN_FUNCTIONS_VISIBLE}      True
