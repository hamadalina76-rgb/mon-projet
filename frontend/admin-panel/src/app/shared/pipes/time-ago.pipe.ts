// src/app/shared/pipes/time-ago.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';
import { formatDistanceToNow, parseISO, isValid } from 'date-fns';
import { fr } from 'date-fns/locale';

@Pipe({
  name: 'timeAgo',
  standalone: true,
})
export class TimeAgoPipe implements PipeTransform {
  transform(value: string | Date | null | undefined): string {
    if (!value) {
      return '-';
    }

    const date = typeof value === 'string' ? parseISO(value) : value;

    if (!isValid(date)) {
      return '-';
    }

    return formatDistanceToNow(date, { addSuffix: true, locale: fr });
  }
}
