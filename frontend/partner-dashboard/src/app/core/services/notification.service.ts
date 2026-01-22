// src/app/core/services/notification.service.ts
import { Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private audio: HTMLAudioElement | null = null;

  constructor(private toastr: ToastrService) {
    this.initAudio();
  }

  private initAudio(): void {
    if (typeof window !== 'undefined') {
      this.audio = new Audio('assets/sounds/notification.mp3');
    }
  }

  success(message: string, title = 'Succès'): void {
    this.toastr.success(message, title);
  }

  error(message: string, title = 'Erreur'): void {
    this.toastr.error(message, title);
  }

  warning(message: string, title = 'Attention'): void {
    this.toastr.warning(message, title);
  }

  info(message: string, title = 'Information'): void {
    this.toastr.info(message, title);
  }

  newOrderAlert(orderNumber: string): void {
    this.playSound();
    this.toastr.info(
      `Nouvelle commande #${orderNumber}`,
      '🔔 Nouvelle commande!',
      {
        timeOut: 10000,
        tapToDismiss: true,
        closeButton: true,
      }
    );
  }

  playSound(): void {
    if (this.audio) {
      this.audio.currentTime = 0;
      this.audio.play().catch(() => {
        // Autoplay blocked
      });
    }
  }

  async requestBrowserPermission(): Promise<boolean> {
    if (!('Notification' in window)) {
      return false;
    }

    if (Notification.permission === 'granted') {
      return true;
    }

    const permission = await Notification.requestPermission();
    return permission === 'granted';
  }

  showBrowserNotification(title: string, body: string): void {
    if (Notification.permission === 'granted') {
      new Notification(title, {
        body,
        icon: 'assets/images/logo-icon.png',
      });
    }
  }
}
