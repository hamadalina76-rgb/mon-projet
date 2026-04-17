import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, distinctUntilChanged, interval, map, startWith } from 'rxjs';
import { TimerFormatPipe } from '@shared/pipes/timer-format.pipe';
import { NotificationService } from '@core/services/notification.service';
import { PrepTimerSessionService } from '../../services/prep-timer-session.service';

@Component({
  selector: 'app-timer',
  standalone: true,
  imports: [CommonModule, TimerFormatPipe],
  templateUrl: './timer.component.html',
  styleUrls: ['./timer.component.scss'],
})
export class TimerComponent implements OnInit, OnDestroy, OnChanges {
  private notif = inject(NotificationService);
  private prepSession = inject(PrepTimerSessionService);

  @Input({ required: true }) startTimeIso!: string;
  @Input({ required: true }) durationMinutes!: number;
  @Input() active = true;
  @Input() orderId?: string;

  @Output() expiredChange = new EventEmitter<boolean>();

  remainingSeconds = 0;

  private subscription?: Subscription;
  private lastExpiredNotified: boolean | null = null;

  ngOnInit(): void {
    this.startTick();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.subscription) return;
    if (changes['startTimeIso'] || changes['durationMinutes'] || changes['active']) {
      this.applyTick(this.calculateRemaining());
    }
    if (changes['active'] && !this.active) {
      this.emitExpiredIfChanged(false);
    }
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  private startTick(): void {
    this.subscription = interval(1000)
      .pipe(
        startWith(0),
        map(() => this.calculateRemaining()),
        distinctUntilChanged(),
      )
      .subscribe((sec) => this.applyTick(sec));
  }

  private calculateRemaining(): number {
    if (!this.active || !this.startTimeIso || this.durationMinutes == null || this.durationMinutes <= 0) {
      return 0;
    }
    const startMs = Date.parse(this.startTimeIso);
    if (Number.isNaN(startMs)) return 0;
    const endMs = startMs + this.durationMinutes * 60_000;
    return Math.ceil((endMs - Date.now()) / 1000);
  }

  private applyTick(sec: number): void {
    const prev = this.remainingSeconds;
    this.remainingSeconds = sec;
    const expired = !this.active ? false : sec <= 0;
    this.emitExpiredIfChanged(expired);

    const crossed =
      this.active &&
      this.orderId &&
      prev > 0 &&
      sec <= 0;
    const oid = this.orderId;
    if (crossed && oid && !this.prepSession.hasAlertPlayed(oid)) {
      this.notif.playPrepDeadlineSound();
      this.prepSession.markAlertPlayed(oid);
    }
  }

  private emitExpiredIfChanged(expired: boolean): void {
    if (this.lastExpiredNotified === expired) return;
    this.lastExpiredNotified = expired;
    this.expiredChange.emit(expired);
  }
}
