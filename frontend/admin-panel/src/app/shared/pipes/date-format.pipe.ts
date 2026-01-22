// src/app/shared/pipes/date-format.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';
import { format, parseISO, isValid } from 'date-fns';
import { fr } from 'date-fns/locale';

@Pipe({
  name: 'dateFormat',
  standalone: true,
})
export class DateFormatPipe implements PipeTransform {
  transform(
    value: string | Date | null | undefined,
    formatStr = 'dd/MM/yyyy HH:mm'
  ): string {
    if (!value) {
      return '-';
    }

    const date = typeof value === 'string' ? parseISO(value) : value;

    if (!isValid(date)) {
      return '-';
    }

    return format(date, formatStr, { locale: fr });
  }
}
