# Infrastructure Terraform (GCP)

Structure:
- modules/: modules réutilisables (Artifact Registry, IAM, Cloud SQL, Pub/Sub, Cloud Run, Envoy)
- envs/: environnements (dev/staging/prod)
- scripts/: scripts utilitaires (validation, génération config Envoy)

Bonnes pratiques:
- Remote state dans GCS (bucket tfstate par environnement)
- Pas de secrets dans terraform.tfvars (préférer Secret Manager ou CI variables)

## Data-services VM (Redis + ClickHouse)

Le module `modules/data_services_vm` permet de préparer une VM privée Compute Engine
pour héberger Redis et ClickHouse via Docker (provisioning Ansible).

Rôle de chaque couche:
- Terraform crée la VM, les tags réseau et les règles firewall minimales.
- Ansible installe Docker sur la VM puis déploie le `docker-compose.yml`.
- L'application ne bascule pas automatiquement sur cette VM: l'objectif initial est
  de préparer l'infrastructure sans casser l'existant.

Flux cible:
1. Terraform crée ou met à jour la VM privée et ouvre seulement les ports data requis.
2. Ansible se connecte à la VM et y rend le stack Redis + ClickHouse exécutable.
3. La validation applicative se fait ensuite séparément avant toute migration réelle.

Objectif de rollout:
- DEV: préparation infra uniquement (backward compatible, Memorystore conservé)
- STAGING: activation pilotée via variables CI
- PROD: réutilisation ultérieure avec les mêmes jobs

Variables CI/CD de contrôle (dans `.gitlab-ci.yml`):
- `DATA_SERVICES_ENV_TARGET` (`dev|staging|prod`)
- `ENABLE_DATA_SERVICES_VM` (`true|false`) pour exporter `TF_VAR_enable_data_services_vm`
- `ENABLE_DATA_SERVICES_ANSIBLE_DEPLOY` (`true|false`) pour autoriser le job Ansible
- `DATA_SERVICES_VM_NAME` et `DATA_SERVICES_VM_ZONE` pour vérification pré-deploy Ansible

Comment lancer le pipeline pour tester Ansible en DEV:
1. Ouvrir GitLab puis `CI/CD > Pipelines > Run pipeline`.
2. Choisir la branche de travail autorisée par le `workflow` CI.
3. Renseigner au minimum les variables suivantes:
   - `DATA_SERVICES_ENV_TARGET=dev`
   - `ENABLE_DATA_SERVICES_VM=true`
   - `ENABLE_DATA_SERVICES_ANSIBLE_DEPLOY=true`
   - `DATA_SERVICES_VM_NAME=data-services-dev`
   - `DATA_SERVICES_VM_ZONE=europe-west1-b`
4. Créer un nouveau pipeline. Ne pas utiliser `Retry` sur un ancien pipeline si les variables ont changé.

Séquence pipeline recommandée:
1. `terraform_validate_data_services`
2. `terraform_plan_data_services` (artifact du plan)
3. `terraform_apply_data_services`
4. `ansible_syntax_check_data_services`
5. `ansible_deploy_data_services` (après infra)

Important:
- Tant que le switch applicatif n'est pas validé, ne pas retirer Memorystore dans DEV.
- Le job Ansible ne remplace pas Terraform: il configure l'OS et les conteneurs sur une VM déjà créée.
- Si la VM reste privée, l'exécution Ansible doit se faire depuis un runner interne, un bastion
  ou via IAP SSH.
- Si `ENABLE_DATA_SERVICES_VM=false`, aucun job Terraform data-services ne sera créé.
- Si `ENABLE_DATA_SERVICES_ANSIBLE_DEPLOY=false`, aucun job Ansible data-services ne sera créé.
