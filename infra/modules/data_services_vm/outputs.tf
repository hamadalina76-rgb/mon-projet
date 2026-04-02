output "name" {
  description = "Compute Engine VM name"
  value       = google_compute_instance.this.name
}

output "zone" {
  description = "Compute Engine VM zone"
  value       = google_compute_instance.this.zone
}

output "private_ip" {
  description = "Private IP address of VM"
  value       = google_compute_instance.this.network_interface[0].network_ip
}

output "self_link" {
  description = "Self link of VM"
  value       = google_compute_instance.this.self_link
}
