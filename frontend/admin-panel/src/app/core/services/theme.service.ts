// src/app/core/services/theme.service.ts
import { Injectable, signal, effect } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'admin_theme';
  
  // Signal pour le thème actuel
  currentTheme = signal<Theme>(this.getStoredTheme());

  constructor() {
    // Effet pour appliquer le thème quand il change
    effect(() => {
      this.applyTheme(this.currentTheme());
    });
    
    // Appliquer le thème initial
    this.applyTheme(this.currentTheme());
  }

  /**
   * Récupère le thème stocké dans localStorage
   */
  private getStoredTheme(): Theme {
    const stored = localStorage.getItem(this.THEME_KEY);
    return (stored === 'dark' || stored === 'light') ? stored : 'light';
  }

  /**
   * Toggle entre light et dark
   */
  toggleTheme(): void {
    const newTheme: Theme = this.currentTheme() === 'light' ? 'dark' : 'light';
    this.setTheme(newTheme);
  }

  /**
   * Définit un thème spécifique
   */
  setTheme(theme: Theme): void {
    this.currentTheme.set(theme);
    localStorage.setItem(this.THEME_KEY, theme);
  }

  /**
   * Applique le thème au DOM
   */
  private applyTheme(theme: Theme): void {
    const html = document.documentElement;
    
    if (theme === 'dark') {
      html.classList.add('dark');
      html.setAttribute('data-theme', 'dark');
    } else {
      html.classList.remove('dark');
      html.setAttribute('data-theme', 'light');
    }
  }

  /**
   * Vérifie si le mode sombre est actif
   */
  isDarkMode(): boolean {
    return this.currentTheme() === 'dark';
  }
}
