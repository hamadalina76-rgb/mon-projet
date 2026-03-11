# Frontend DevOps Conventions (Speedline)

## Folder structure

- `frontend/partner-dashboard` (Angular SPA)
- `frontend/admin-panel` (Angular SPA)
- `frontend/courier_app` (Flutter Web)
- `frontend/customer_app` (Flutter Web)
- `infra/envs/dev` (DEV terraform deployment)
- `infra/envs/staging` (staging pattern)
- `infra/envs/prod` (prod pattern)

## Runtime configuration convention

All frontends use runtime `config.json` (no rebuild required per environment):

- Angular: `src/assets/config/config.json`
- Flutter Web: `assets/config/config.json` (injected into built web assets at container startup)

Mandatory keys:

- `env`
- `apiBaseUrl`
- `wsUrl`

Admin-panel also uses:

- `apiUrl`
- `uploadsBaseUrl`
- `notificationsApiUrl`

## API integration rule

Use only API Gateway URL:

- DEV: `https://api-gateway-392205979525.europe-west1.run.app`

Do not call internal microservice URLs directly from frontend code.

## Docker standards

Each frontend contains:

- `Dockerfile.dev`
- `Dockerfile`
- `Caddyfile`
- `docker-entrypoint.sh`

`Dockerfile.dev` runs dev server with hot reload.
`Dockerfile` builds static assets and serves with Caddy on Cloud Run `$PORT`.

## Terraform standards

DEV frontend services are defined in `infra/envs/dev/main.tf`:

- `partner-dashboard`
- `admin-panel`
- `courier-app`
- `customer-app`

Use these variables for environment promotion:

- `frontend_env`
- `frontend_api_base_url`
- `frontend_ws_url`

Replicate for staging/prod by copying env tfvars and setting scoped values.

## GitLab CI/CD standards

DEV pipeline builds and deploys frontend images:

- `build_frontend_dev_images`
- `terraform_apply_dev` (merges backend + frontend image maps)

Manual-ready placeholders:

- `build_frontend_staging_images`
- `terraform_apply_staging`
- `build_frontend_prod_images`
- `terraform_apply_prod`

## Required CI variables (environment-scoped)

- `GCP_PROJECT_ID`
- `GCP_REGION`
- `WIF_PROVIDER`
- `GCP_SA_EMAIL`
- `ARTIFACT_REPO`
- `TF_DIR`
- `API_BASE_URL` (optional; defaults to API Gateway URL in app entrypoint)
- `WS_URL` (optional)
