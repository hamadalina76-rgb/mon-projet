import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timerFormat',
  standalone: true,
})
export class TimerFormatPipe implements PipeTransform {
  transform(totalSeconds: number | null | undefined): string {
    const s = Math.max(0, Math.floor(Number(totalSeconds) || 0));
    if (s >= 3600) {
      const h = Math.floor(s / 3600);
      const m = Math.floor((s % 3600) / 60);
      return `${h}h ${String(m).padStart(2, '0')}m`;
    }
    const m = Math.floor(s / 60);
    const r = s % 60;
    return `${String(m).padStart(2, '0')}:${String(r).padStart(2, '0')}`;
  }
}
