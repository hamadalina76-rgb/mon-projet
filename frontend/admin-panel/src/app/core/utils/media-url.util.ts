import { environment } from '@environments/environment';

const env = environment as { uploadsBaseUrl?: string };
const UPLOADS_BASE = env.uploadsBaseUrl ?? 'http://localhost:8080';

/**
 * Transforme une URL d'image stockée (ex: profil) pour qu'elle soit accessible depuis l'admin panel.
 * Les URLs venant de l'app mobile peuvent contenir 10.0.2.2 (émulateur Android) qui n'est pas
 * accessible depuis un navigateur desktop. On utilise uploadsBaseUrl (gateway) à la place.
 */
export function getMediaUrl(url: string | null | undefined): string | null {
  if (!url || typeof url !== 'string') {
    return null;
  }
  const trimmed = url.trim();
  if (!trimmed) return null;

  // Extraire le chemin /uploads/xxx (ou uploads/xxx)
  const uploadsMatch = trimmed.match(/(\/?uploads\/[^?\s#]+)/);
  if (uploadsMatch) {
    let path = uploadsMatch[1];
    if (!path.startsWith('/')) path = '/' + path;
    return UPLOADS_BASE.replace(/\/$/, '') + path;
  }

  return trimmed;
}
