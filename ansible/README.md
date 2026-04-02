# Ansible - Data Services VM (Redis + ClickHouse)

This directory provisions and deploys a Docker stack on a private Compute Engine VM.

Ce dossier complète Terraform:
- Terraform prépare la VM, le réseau et les règles firewall.
- Ansible prépare l'OS invité et y déploie Redis + ClickHouse.
- Le compose généré ici reste volontairement simple pour garder un rollout lisible et réversible.

## Inventory

Edit `inventory/hosts.yml` with real private IPs and project/zone, values.

Lecture rapide de l'inventory:
- `dev`, `staging`, `prod` sont les groupes ciblés par `--limit`.
- chaque host représente la VM data-services d'un environnement.
- `ansible_host` doit être l'IP privée réellement attribuée par Terraform.

## Variables

Environment-specific defaults are under `group_vars/`:
- `dev.yml`
- `staging.yml`
- `prod.yml`

Set secure values (especially passwords) via Ansible Vault or CI variables, not plain files.

Variables importantes:
- `compose_project_dir`: répertoire cible sur la VM où le compose et les volumes seront créés.
- `redis_host_port`, `clickhouse_http_port`, `clickhouse_native_port`: ports publiés sur la VM.
- `redis_password` / `clickhouse_password`: secrets à sortir des fichiers versionnés avant usage réel.

## Run

```bash
ansible-playbook -i inventory/hosts.yml playbooks/data-services.yml --limit dev
```

Ce playbook fait, dans l'ordre:
1. installation des paquets système nécessaires à Docker;
2. démarrage et activation du service Docker;
3. création des répertoires persistants pour Redis et ClickHouse;
4. rendu du `docker-compose.yml` depuis le template Jinja;
5. lancement du stack et affichage des conteneurs actifs.

## GitLab CI/CD Integration

Le pipeline inclut:
- `ansible_syntax_check_data_services`
- `ansible_deploy_data_services`

Variables utiles:
- `DATA_SERVICES_ENV_TARGET=dev|staging|prod`
- `ENABLE_DATA_SERVICES_VM=true` pour rendre la chaîne Terraform data-services visible
- `ENABLE_DATA_SERVICES_ANSIBLE_DEPLOY=true` pour rendre le job deploy exécutable
- `DATA_SERVICES_VM_NAME` et `DATA_SERVICES_VM_ZONE` pour vérification de la VM avant exécution

Exemple minimal pour tester `ansible_deploy_data_services` via GitLab UI:
1. `Run pipeline`
2. variables:
   - `DATA_SERVICES_ENV_TARGET=dev`
   - `ENABLE_DATA_SERVICES_VM=true`
   - `ENABLE_DATA_SERVICES_ANSIBLE_DEPLOY=true`
   - `DATA_SERVICES_VM_NAME=data-services-dev`
   - `DATA_SERVICES_VM_ZONE=europe-west1-b`
3. vérifier ensuite l'ordre:
   - `terraform_validate_data_services`
   - `terraform_plan_data_services`
   - `terraform_apply_data_services`
   - `ansible_syntax_check_data_services`
   - `ansible_deploy_data_services`

Le job deploy Ansible est ordonné après l'étape Terraform data-services pour limiter le risque de drift.

## Notes

- Compose images are aligned with local `backend/docker-compose.yml`:
  - `redis:7-alpine`
  - `clickhouse/clickhouse-server:23.8`
- For private-only hosts, run from an internal runner/bastion or use IAP SSH tunneling.
- En DEV, cette stack est une préparation d'infra: Memorystore peut rester la source active
  tant que la bascule applicative n'a pas été testée.
