import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

/**
 * Composant de page liste générique - même design que partners-list/admins-list.
 * Utilise la projection de contenu pour le header, les filtres, le tableau et les actions.
 */
@Component({
  selector: 'app-list-page',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './list-page.component.html',
  styleUrls: ['./list-page.component.scss'],
})
export class ListPageComponent {
  /** Titre de la page (clé de traduction) */
  @Input() title = '';

  /** Afficher le bouton retour */
  @Input() showBackButton = false;

  /** Lien du bouton retour */
  @Input() backLink = '/dashboard';

  /** État de chargement */
  @Input() loading = false;

  /** État vide (aucune donnée) */
  @Input() empty = false;

  /** Message ou clé de traduction pour l'état vide */
  @Input() emptyMessage = 'common.noData';

  /** Icône pour l'état vide */
  @Input() emptyIcon = 'inbox';

  /** Etat d'erreur (affiché avant loading/empty) */
  @Input() error: string | null = null;

  /** Pagination : total d'éléments */
  @Input() totalItems = 0;

  /** Pagination : page courante (1-based) */
  @Input() currentPage = 1;

  /** Pagination : éléments par page */
  @Input() itemsPerPage = 20;

  /** Pagination : options pour le sélecteur */
  @Input() itemsPerPageOptions: number[] = [20, 50, 100];

  /** Pagination : afficher la pagination */
  @Input() showPagination = true;

  /** Pagination : libellé "Éléments par page" */
  @Input() itemsPerPageLabel = 'partners.itemsPerPage';

  /** Pagination : libellé "sur" / "of" */
  @Input() ofLabel = 'partners.of';

  @Output() pageChange = new EventEmitter<{
    page: number;
    pageSize: number;
  }>();

  @Output() itemsPerPageChange = new EventEmitter<number>();

  Math = Math;

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.itemsPerPage));
  }

  onItemsPerPageChange(value: number): void {
    this.itemsPerPageChange.emit(value);
    this.pageChange.emit({ page: 1, pageSize: value });
  }

  goToFirstPage(): void {
    if (this.currentPage > 1) {
      this.pageChange.emit({ page: 1, pageSize: this.itemsPerPage });
    }
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.pageChange.emit({ page: this.currentPage - 1, pageSize: this.itemsPerPage });
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.pageChange.emit({ page: this.currentPage + 1, pageSize: this.itemsPerPage });
    }
  }

  goToLastPage(): void {
    if (this.currentPage < this.totalPages) {
      this.pageChange.emit({ page: this.totalPages, pageSize: this.itemsPerPage });
    }
  }
}
