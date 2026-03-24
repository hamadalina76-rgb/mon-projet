// src/main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

function shouldRecoverFromChunkError(errorLike: unknown): boolean {
  const msg = String((errorLike as any)?.message ?? errorLike ?? '').toLowerCase();
  return (
    msg.includes('loading dynamically imported module') ||
    msg.includes('failed to fetch dynamically imported module') ||
    msg.includes('chunkloaderror')
  );
}

function recoverFromChunkErrorOnce(): void {
  const key = 'speedline_admin_chunk_recover_once';
  try {
    if (sessionStorage.getItem(key) === '1') return;
    sessionStorage.setItem(key, '1');
    // Force a hard navigation to refresh lazy chunk URLs.
    window.location.reload();
  } catch {
    window.location.reload();
  }
}

window.addEventListener('error', (event) => {
  if (shouldRecoverFromChunkError((event as ErrorEvent)?.error || event.message)) {
    recoverFromChunkErrorOnce();
  }
});

window.addEventListener('unhandledrejection', (event) => {
  if (shouldRecoverFromChunkError((event as PromiseRejectionEvent)?.reason)) {
    recoverFromChunkErrorOnce();
  }
});

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
