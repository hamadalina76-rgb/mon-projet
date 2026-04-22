import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PERMISSIONS } from '@core/models/role.model';
import { AuthService } from '@core/services/auth.service';
import type {
  BundlingConfigDto,
  ExclusivityConfigDto,
  GeneralConfigDto,
  InternalExternalConfigDto,
  ScoringComponentRow,
} from '../models/dispatch-config.model';
import { DispatchApiService } from '../services/dispatch-api.service';
import { DispatchConfigStateService } from '../services/dispatch-config-state.service';

@Component({
  selector: 'app-dispatch-config-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    DragDropModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatIconModule,
    MatDividerModule,
    MatTooltipModule,
    MatTableModule,
    TranslateModule,
  ],
  templateUrl: './dispatch-config-page.component.html',
  styleUrls: ['./dispatch-config-page.component.scss'],
})
export class DispatchConfigPageComponent implements OnInit {
  readonly state = inject(DispatchConfigStateService);
  private api = inject(DispatchApiService);
  private auth = inject(AuthService);

  readonly canManage = computed(() =>
    this.auth.hasAnyPermission([PERMISSIONS.DELIVERY_MANAGE])
  );

  scoringRows = signal<ScoringComponentRow[]>([]);
  draftGeneral: GeneralConfigDto = {};
  draftIe: InternalExternalConfigDto = {};
  draftBundling: BundlingConfigDto = {};
  draftExclusivity: ExclusivityConfigDto = { cells: [], partnerOverrides: [] };

  simZoneId = signal<number | null>(null);
  simResult = signal('');
  replayId = signal('');
  replayOut = signal('');
  auditFrom = signal('');
  auditTo = signal('');

  ngOnInit(): void {
    this.reload();
  }

  reload() {
    this.state.refreshAll().subscribe({
      next: () => this.syncDrafts(),
      error: (err) => console.error('Dispatch config refresh failed', err),
    });
  }

  syncDrafts() {
    const g = this.state.general();
    if (g) {
      Object.assign(this.draftGeneral, g);
    }
    const ie = this.state.internalExternal();
    if (ie) {
      Object.assign(this.draftIe, ie);
    }
    const b = this.state.bundling();
    if (b) {
      Object.assign(this.draftBundling, b);
    }
    const x = this.state.exclusivity();
    if (x) {
      this.draftExclusivity = {
        cells: x.cells ? [...x.cells] : [],
        partnerOverrides: x.partnerOverrides ? [...x.partnerOverrides] : [],
      };
    }
    const s = this.state.scoring();
    if (s?.components?.length) {
      const rows = [...s.components].sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
      this.scoringRows.set(rows);
    }
  }

  drop(event: CdkDragDrop<ScoringComponentRow[]>) {
    const list = [...this.scoringRows()];
    moveItemInArray(list, event.previousIndex, event.currentIndex);
    list.forEach((r, i) => (r.order = i + 1));
    this.scoringRows.set(list);
  }

  saveScoringOrder() {
    this.state.saveScoring({ components: this.scoringRows() }).subscribe(() => this.syncDrafts());
  }

  saveGeneral() {
    this.state.saveGeneral({ ...this.draftGeneral }).subscribe(() => this.syncDrafts());
  }

  saveIe() {
    this.state.saveInternalExternal({ ...this.draftIe }).subscribe(() => this.syncDrafts());
  }

  saveBundling() {
    this.state.saveBundling({ ...this.draftBundling }).subscribe(() => this.syncDrafts());
  }

  saveExclusivity() {
    this.state.saveExclusivity({ ...this.draftExclusivity }).subscribe(() => this.syncDrafts());
  }

  runSim() {
    this.api
      .simulateDispatch({
        zoneId: this.simZoneId() ?? undefined,
        scoringOverlay: { components: this.scoringRows() },
      })
      .subscribe((r) => this.simResult.set(JSON.stringify(r, null, 2)));
  }

  runReplay() {
    const id = this.replayId().trim();
    if (!id) {
      return;
    }
    this.api.replayDispatch(id).subscribe((r) => this.replayOut.set(JSON.stringify(r, null, 2)));
  }

  addExclusivityCell() {
    const cells = this.draftExclusivity.cells ?? [];
    this.draftExclusivity.cells = [...cells, { zoneId: 1, commerceType: 'FOOD', allowed: true }];
  }

  addPartnerOverride() {
    const list = this.draftExclusivity.partnerOverrides ?? [];
    this.draftExclusivity.partnerOverrides = [
      ...list,
      { partnerId: 1, zoneId: 1, commerceType: 'FOOD', allowed: true },
    ];
  }

  exportAudit() {
    const from = this.auditFrom().trim();
    const to = this.auditTo().trim();
    if (!from || !to) {
      return;
    }
    this.api.exportDispatchConfigAudit(from, to).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'dispatch-config-audit.csv';
      a.click();
      URL.revokeObjectURL(url);
    });
  }
}
