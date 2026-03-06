// src/app/shared/components/image-picker/image-picker.component.ts — Angular 19 standalone
import {
  Component, input, signal, computed, forwardRef, inject, OnInit, effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '@core/services/api.service';

interface UploadResponse { url: string; }

@Component({
  selector: 'app-image-picker',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './image-picker.component.html',
  styleUrls: ['./image-picker.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ImagePickerComponent),
      multi: true,
    },
  ],
})
export class ImagePickerComponent implements ControlValueAccessor, OnInit {
  private api = inject(ApiService);

  // ─── Inputs ──────────────────────────────────────────────────────────────
  /** Endpoint de l'API pour l'upload de fichier, ex: 'partners/47/upload/logo' */
  uploadEndpoint = input<string>('');
  /** Libellé affiché dans le champ URL */
  label         = input<string>('Image');
  /** Placeholder du champ URL */
  placeholder   = input<string>('https://…');

  // ─── Internal state ───────────────────────────────────────────────────────
  mode      = signal<'url' | 'file'>('url');
  urlInput  = signal<string>('');
  preview   = signal<string | null>(null);
  uploading = signal(false);
  error     = signal<string | null>(null);
  isDragging = signal(false);
  disabled  = signal(false);

  canUpload = computed(() => this.uploadEndpoint().length > 0);

  // ─── ControlValueAccessor ────────────────────────────────────────────────
  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  ngOnInit(): void {
    // keep preview in sync with urlInput
    effect(() => {
      const val = this.urlInput();
      if (this.mode() === 'url') {
        this.preview.set(val || null);
      }
    }, { allowSignalWrites: true });
  }

  writeValue(value: string): void {
    const safe = value || '';
    this.urlInput.set(safe);
    this.preview.set(safe || null);
  }

  registerOnChange(fn: (v: string) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
  setDisabledState(isDisabled: boolean): void { this.disabled.set(isDisabled); }

  // ─── Handlers ─────────────────────────────────────────────────────────────

  setMode(m: 'url' | 'file'): void {
    this.mode.set(m);
    this.error.set(null);
  }

  onUrlInput(event: Event): void {
    const val = (event.target as HTMLInputElement).value;
    this.urlInput.set(val);
    this.preview.set(val || null);
    this.onChange(val);
    this.onTouched();
  }

  clearImage(): void {
    this.urlInput.set('');
    this.preview.set(null);
    this.onChange('');
    this.onTouched();
  }

  // ─── Drag & Drop ──────────────────────────────────────────────────────────

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave(): void {
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) this.processFile(file);
  }

  // ─── File picker ──────────────────────────────────────────────────────────

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.processFile(file);
    // reset so the same file can be re-selected
    (event.target as HTMLInputElement).value = '';
  }

  private processFile(file: File): void {
    this.error.set(null);

    // Client-side validation
    const ALLOWED = ['image/jpeg', 'image/png', 'image/webp'];
    if (!ALLOWED.includes(file.type)) {
      this.error.set('Format non supporté. Utilisez JPG, PNG ou WEBP.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.error.set('Taille max 5 Mo dépassée.');
      return;
    }

    // Local preview immediately
    const reader = new FileReader();
    reader.onload = (e) => this.preview.set(e.target?.result as string);
    reader.readAsDataURL(file);

    if (!this.canUpload()) {
      // No upload endpoint — use data URL as the value (useful in dev/mock contexts)
      reader.onloadend = () => {
        const url = reader.result as string;
        this.urlInput.set(url);
        this.onChange(url);
      };
      return;
    }

    // Upload
    this.uploading.set(true);
    const fd = new FormData();
    fd.append('file', file);

    this.api.upload<UploadResponse>(this.uploadEndpoint(), fd).subscribe({
      next: (res) => {
        this.uploading.set(false);
        this.urlInput.set(res.url);
        this.preview.set(res.url);
        this.onChange(res.url);
        this.onTouched();
      },
      error: (err) => {
        this.uploading.set(false);
        const msg: string = err?.error?.message ?? err?.message ?? 'Erreur lors de l\'upload.';
        this.error.set(msg);
      },
    });
  }
}
