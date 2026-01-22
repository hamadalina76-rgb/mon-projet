// src/app/features/finance/payouts/payouts.component.ts - Angular 19
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { FinanceService } from '../services/finance.service';

@Component({
  selector: 'app-payouts',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    TranslateModule,
  ],
  templateUrl: './payouts.component.html',
  styleUrls: ['./payouts.component.scss'],
})
export class PayoutsComponent implements OnInit {
  private fb = inject(FormBuilder);
  private financeService = inject(FinanceService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  // Angular 19 Signals
  loading = signal(false);
  requesting = signal(false);
  showPayoutForm = signal(false);
  availableBalance = signal(0);
  payouts = signal<any[]>([]);
  
  bankAccount = signal({
    bankName: '',
    accountHolder: '',
    accountNumber: '',
    rib: '',
  });

  // Computed
  canRequestPayout = computed(() => this.availableBalance() >= 100);
  
  displayedColumns = ['date', 'amount', 'status', 'reference', 'actions'];

  payoutForm: FormGroup = this.fb.group({
    amount: [0, [Validators.required, Validators.min(100)]],
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    
    this.financeService.getPayouts().subscribe({
      next: (data) => {
        this.availableBalance.set(data.availableBalance);
        this.payouts.set(data.payouts);
        this.bankAccount.set(data.bankAccount);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading payouts:', err);
        this.loading.set(false);
      }
    });
  }

  requestPayout(): void {
    if (this.payoutForm.invalid) return;
    
    this.requesting.set(true);
    const amount = this.payoutForm.value.amount;
    
    this.financeService.requestPayout(amount).subscribe({
      next: (payout) => {
        this.payouts.update(p => [payout, ...p]);
        this.availableBalance.update(b => b - amount);
        this.showPayoutForm.set(false);
        this.payoutForm.reset();
        this.requesting.set(false);
        this.snackBar.open('Demande de versement envoyée', 'OK', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error requesting payout:', err);
        this.requesting.set(false);
        this.snackBar.open('Erreur lors de la demande', 'OK', { duration: 3000 });
      }
    });
  }

  downloadInvoice(payoutId: string): void {
    this.financeService.downloadInvoice(payoutId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `facture-${payoutId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Error downloading invoice:', err)
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'completed': return 'completed';
      case 'pending': return 'pending';
      case 'processing': return 'processing';
      case 'failed': return 'failed';
      default: return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'completed': return 'Effectué';
      case 'pending': return 'En attente';
      case 'processing': return 'En cours';
      case 'failed': return 'Échoué';
      default: return status;
    }
  }
}
