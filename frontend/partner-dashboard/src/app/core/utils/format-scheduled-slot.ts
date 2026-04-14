import { formatDate } from '@angular/common';

/** Locale Angular pour formatDate selon la langue UI. */
function localeForLang(lang: string): string {
  const l = (lang || 'fr').split('-')[0]?.toLowerCase() ?? 'fr';
  if (l === 'ar') return 'ar-TN';
  if (l === 'en') return 'en-GB';
  return 'fr-FR';
}

/** Affiche le créneau planifié (date + heure) pour toasts / rappels. */
export function formatScheduledSlot(iso: string | undefined, lang: string): string {
  if (!iso?.trim()) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return formatDate(d, 'EEEE d MMM yyyy, HH:mm', localeForLang(lang));
}
