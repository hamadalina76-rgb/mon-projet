// src/app/shared/pipes/currency.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'tndCurrency',
  standalone: true,
})
export class TndCurrencyPipe implements PipeTransform {
  transform(value: number | string, showSymbol = true): string {
    if (value === null || value === undefined) {
      return '';
    }

    const numValue = typeof value === 'string' ? parseFloat(value) : value;

    if (isNaN(numValue)) {
      return '';
    }

    const formatted = numValue.toFixed(3).replace(/\B(?=(\d{3})+(?!\d))/g, ' ');

    return showSymbol ? `${formatted} TND` : formatted;
  }
}
