terraform {
  required_providers {
    mongodbatlas = {
      source  = "mongodb/mongodbatlas"
      version = "~> 1.20"
    }
  }
}

provider "mongodbatlas" {
  public_key  = var.atlas_public_key
  private_key = var.atlas_private_key
}

locals {
  manual_mode = var.manage_resources == false
}

# NOTE:
# M0/FREE Atlas provisioning via Terraform can be constrained by provider/API limits
# depending on account and Atlas capabilities. In this repository we keep
# manual-managed mode by default (manage_resources=false) and centralize inputs/outputs.
#
# When/if full provisioning is enabled, add mongodbatlas resources here guarded by:
#   count = var.manage_resources ? 1 : 0
