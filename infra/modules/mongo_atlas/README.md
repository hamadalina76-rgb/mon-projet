# mongo_atlas module

This module is wired for **manual-managed Atlas mode by default** (`manage_resources = false`).

Why:
- Atlas M0/FREE provisioning may be restricted or vary by provider/API/account capabilities.
- We keep infra reproducible by storing canonical inputs/outputs in Terraform.

## Inputs
- `atlas_public_key`, `atlas_private_key`, `atlas_org_id`
- `atlas_project_id`, `atlas_cluster_name`, `atlas_region`
- `mongo_db_name`, `mongo_db_username`, `mongo_db_password`, `mongo_allowed_cidr`
- `mongodb_uri` (sensitive runtime URI used by Cloud Run)

## Current behavior
- No Atlas resources are created when `manage_resources = false`.
- Outputs expose the selected Atlas metadata and sensitive `mongodb_uri`.

## Future extension
- Add provider-backed resources (`mongodbatlas_project`, `mongodbatlas_cluster`,
  `mongodbatlas_database_user`, `mongodbatlas_project_ip_access_list`) guarded with
  `count = var.manage_resources ? 1 : 0`.
