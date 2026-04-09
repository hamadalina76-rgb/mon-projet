import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { OrdersService } from './orders.service';

/**
 * Récupère le ticket HTML depuis l’API (order-service) et l’imprime dans un iframe masqué
 * (le contenu imprimé est celui du ticket, pas la page du dashboard).
 */
@Injectable({ providedIn: 'root' })
export class KitchenPrintService {
  private orders = inject(OrdersService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  printKitchenTicket(orderId: string): void {
    if (typeof document === 'undefined' || !orderId) return;

    this.orders.getKitchenTicketHtml(orderId).subscribe({
      next: (html) => this.printHtmlInHiddenIframe(html),
      error: () => {
        this.snackBar.open(
          this.translate.instant('ORDERS.KITCHEN_PRINT_ERROR'),
          'OK',
          { duration: 4500 },
        );
      },
    });
  }

  private printHtmlInHiddenIframe(html: string): void {
    const iframe = document.createElement('iframe');
    iframe.setAttribute('aria-hidden', 'true');
    iframe.title = 'kitchen-ticket-print';
    iframe.style.cssText =
      'position:fixed;right:0;bottom:0;width:0;height:0;border:0;opacity:0;pointer-events:none;visibility:hidden';

    document.body.appendChild(iframe);

    const win = iframe.contentWindow;
    const doc = iframe.contentDocument;
    if (!win || !doc) {
      iframe.remove();
      return;
    }

    doc.open();
    doc.write(html);
    doc.close();

    const cleanup = () => {
      iframe.remove();
    };

    try {
      win.focus();
      win.addEventListener('afterprint', () => cleanup(), { once: true });
      win.print();
      setTimeout(() => {
        if (iframe.parentNode) cleanup();
      }, 90_000);
    } catch {
      cleanup();
    }
  }
}
