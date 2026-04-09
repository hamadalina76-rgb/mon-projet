import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timerFormat',
  standalone: true,
})
export class TimerFormatPipe implements PipeTransform {
  transform(totalSeconds: number | null | undefined): string {
    const s = Math.max(0, Math.floor(Number(totalSeconds) || 0));
    const m = Math.floor(s / 60);
    const r = s % 60;
    return `${String(m).padStart(2, '0')}:${String(r).padStart(2, '0')}`;
  }
}
