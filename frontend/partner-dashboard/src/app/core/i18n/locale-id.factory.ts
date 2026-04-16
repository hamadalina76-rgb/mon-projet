/**
 * Locale Angular (DatePipe, nombres) alignée sur la langue UI.
 * `en-GB` assure l’ordre jour/mois/année même en anglais (évite en-US).
 */
export function partnerAppLocaleId(): string {
  try {
    const lang = (localStorage.getItem('partnerLang') || 'fr').toLowerCase();
    if (lang === 'en') return 'en-GB';
    if (lang === 'ar') return 'ar-TN';
    return 'fr';
  } catch {
    return 'fr';
  }
}
