locals {
  # On réutilise le nom de VM comme tag réseau pour rendre les règles firewall,
  # plus faciles à cibler sans dépendre d'un tag générique partagé.
  instance_tags = distinct(concat(var.network_tags, ["data-services", var.name]))
}

resource "google_compute_instance" "this" {
  project      = var.project_id
  zone         = var.zone
  name         = var.name
  machine_type = var.machine_type
  tags         = local.instance_tags

  allow_stopping_for_update = true

  boot_disk {
    initialize_params {
      image = var.boot_image
      size  = var.boot_disk_size_gb
      type  = var.boot_disk_type
    }
  }

  network_interface {
    network    = var.network
    subnetwork = var.subnetwork != "" ? var.subnetwork : null
    # Pas d'access_config: aucune IP publique n'est attribuée.
    # L'accès doit donc se faire via réseau privé, bastion ou IAP.
  }

  metadata = {
    enable-oslogin = "FALSE"
  }

  metadata_startup_script = var.startup_script != "" ? var.startup_script : null

  dynamic "service_account" {
    for_each = var.service_account_email != "" ? [1] : []
    content {
      email  = var.service_account_email
      scopes = var.service_account_scopes
    }
  }

  labels = var.labels
}

resource "google_compute_firewall" "allow_data_ports_internal" {
  # Ouvre uniquement les ports Redis et ClickHouse aux CIDR explicitement autorisés.
  # Cette règle ne doit pas être interprétée comme une exposition publique.
  project = var.project_id
  name    = "${var.name}-allow-data-internal"
  network = var.network

  direction     = "INGRESS"
  source_ranges = var.allowed_source_ranges
  target_tags   = local.instance_tags

  allow {
    protocol = "tcp"
    ports    = var.data_ports
  }
}

resource "google_compute_firewall" "allow_iap_ssh" {
  count = var.enable_iap_ssh ? 1 : 0

  # Permet l'administration SSH via Google IAP sans IP publique sur la VM.
  project = var.project_id
  name    = "${var.name}-allow-iap-ssh"
  network = var.network

  direction     = "INGRESS"
  source_ranges = ["35.235.240.0/20"]
  target_tags   = local.instance_tags

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
}

resource "google_compute_firewall" "allow_admin_ssh" {
  count = length(var.admin_ssh_source_ranges) > 0 ? 1 : 0

  # Optionnel: utile seulement si une source d'administration privée explicite existe.
  project = var.project_id
  name    = "${var.name}-allow-admin-ssh"
  network = var.network

  direction     = "INGRESS"
  source_ranges = var.admin_ssh_source_ranges
  target_tags   = local.instance_tags

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
}

resource "google_compute_firewall" "allow_egress_internet" {
  # Autorise la VM data-services à sortir vers Internet.
  # Le trafic reste privé côté VM; la sortie effective passe par Cloud NAT.
  project = var.project_id
  name    = "${var.name}-allow-egress-internet"
  network = var.network

  direction          = "EGRESS"
  destination_ranges = ["0.0.0.0/0"]
  target_tags        = local.instance_tags

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }

  allow {
    protocol = "udp"
    ports    = ["53"]
  }

  allow {
    protocol = "tcp"
    ports    = ["53"]
  }

  allow {
    protocol = "icmp"
  }
}
