#!/usr/bin/env bash
set -euo pipefail

echo "==> Terraform fmt"
terraform fmt -recursive

echo "==> Terraform validate (dev)"
( cd infra/envs/dev && terraform validate ) || true

echo "Done."
