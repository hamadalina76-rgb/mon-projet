import { Injectable, inject, signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize, map, tap } from 'rxjs/operators';
import type {
  BundlingConfigDto,
  DispatchConfigMeta,
  ExclusivityConfigDto,
  GeneralConfigDto,
  GroupEnvelope,
  InternalExternalConfigDto,
  ScoringConfigDto,
} from '../models/dispatch-config.model';
import { DispatchApiService } from './dispatch-api.service';

@Injectable({ providedIn: 'root' })
export class DispatchConfigStateService {
  private api = inject(DispatchApiService);
  private snackBar = inject(MatSnackBar);
  private translate = inject(TranslateService);

  readonly meta = signal<DispatchConfigMeta | null>(null);
  readonly general = signal<GeneralConfigDto | null>(null);
  readonly scoring = signal<ScoringConfigDto | null>(null);
  readonly internalExternal = signal<InternalExternalConfigDto | null>(null);
  readonly bundling = signal<BundlingConfigDto | null>(null);
  readonly exclusivity = signal<ExclusivityConfigDto | null>(null);
  readonly loading = signal(false);

  refreshAll() {
    this.loading.set(true);
    return forkJoin({
      g: this.api.getDispatchConfigGeneral(),
      s: this.api.getDispatchConfigScoring(),
      ie: this.api.getDispatchConfigInternalExternal(),
      b: this.api.getDispatchConfigBundling(),
      x: this.api.getDispatchConfigExclusivity(),
    }).pipe(
      tap(({ g, s, ie, b, x }) => {
        this.meta.set(g.meta);
        this.general.set(g.data);
        this.scoring.set(s.data);
        this.internalExternal.set(ie.data);
        this.bundling.set(b.data);
        this.exclusivity.set(x.data);
      }),
      catchError((err) => {
        this.snackBar.open(
          this.translate.instant('dispatchConfig.loadError'),
          this.translate.instant('common.close'),
          { duration: 5000 }
        );
        throw err;
      }),
      finalize(() => this.loading.set(false)),
      map(() => undefined)
    );
  }

  private baseVersion(): number {
    const m = this.meta();
    if (!m) {
      return 0;
    }
    return m.activeVersion;
  }

  saveGeneral(payload: GeneralConfigDto) {
    const prev = this.general();
    this.general.set(payload);
    return this.api.putDispatchConfigGeneral({ baseVersion: this.baseVersion(), data: payload }).pipe(
      tap((env) => {
        this.general.set(env.data);
        this.meta.set(env.meta);
        this.snackBar.open(this.translate.instant('dispatchConfig.saved'), '', { duration: 2500 });
      }),
      catchError((err) => {
        this.general.set(prev);
        this.snackBar.open(this.translate.instant('dispatchConfig.saveError'), '', { duration: 4000 });
        return of(null);
      })
    );
  }

  saveScoring(payload: ScoringConfigDto) {
    const prev = this.scoring();
    this.scoring.set(payload);
    return this.api.putDispatchConfigScoring({ baseVersion: this.baseVersion(), data: payload }).pipe(
      tap((env) => {
        this.scoring.set(env.data);
        this.meta.set(env.meta);
        this.snackBar.open(this.translate.instant('dispatchConfig.saved'), '', { duration: 2500 });
      }),
      catchError(() => {
        this.scoring.set(prev);
        this.snackBar.open(this.translate.instant('dispatchConfig.saveError'), '', { duration: 4000 });
        return of(null);
      })
    );
  }

  saveInternalExternal(payload: InternalExternalConfigDto) {
    const prev = this.internalExternal();
    this.internalExternal.set(payload);
    return this.api.putDispatchConfigInternalExternal({ baseVersion: this.baseVersion(), data: payload }).pipe(
      tap((env) => {
        this.internalExternal.set(env.data);
        this.meta.set(env.meta);
        this.snackBar.open(this.translate.instant('dispatchConfig.saved'), '', { duration: 2500 });
      }),
      catchError(() => {
        this.internalExternal.set(prev);
        this.snackBar.open(this.translate.instant('dispatchConfig.saveError'), '', { duration: 4000 });
        return of(null);
      })
    );
  }

  saveBundling(payload: BundlingConfigDto) {
    const prev = this.bundling();
    this.bundling.set(payload);
    return this.api.putDispatchConfigBundling({ baseVersion: this.baseVersion(), data: payload }).pipe(
      tap((env) => {
        this.bundling.set(env.data);
        this.meta.set(env.meta);
        this.snackBar.open(this.translate.instant('dispatchConfig.saved'), '', { duration: 2500 });
      }),
      catchError(() => {
        this.bundling.set(prev);
        this.snackBar.open(this.translate.instant('dispatchConfig.saveError'), '', { duration: 4000 });
        return of(null);
      })
    );
  }

  saveExclusivity(payload: ExclusivityConfigDto) {
    const prev = this.exclusivity();
    this.exclusivity.set(payload);
    return this.api.putDispatchConfigExclusivity({ baseVersion: this.baseVersion(), data: payload }).pipe(
      tap((env) => {
        this.exclusivity.set(env.data);
        this.meta.set(env.meta);
        this.snackBar.open(this.translate.instant('dispatchConfig.saved'), '', { duration: 2500 });
      }),
      catchError(() => {
        this.exclusivity.set(prev);
        this.snackBar.open(this.translate.instant('dispatchConfig.saveError'), '', { duration: 4000 });
        return of(null);
      })
    );
  }
}
