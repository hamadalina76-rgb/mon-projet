import { Order } from '../models/order.model';

export interface PrepTimerContext {
  startTimeIso: string;
  durationMinutes: number;
}

export function toIsoTimestamp(value: Date | string | undefined | null): string | null {
  if (value == null) return null;
  if (typeof value === 'string') {
    const t = Date.parse(value);
    return Number.isNaN(t) ? null : new Date(t).toISOString();
  }
  return value.toISOString();
}

export function isPrepTimerActiveStatus(status: string | undefined | null): boolean {
  return status === 'CONFIRMED' || status === 'PREPARING';
}

function normalizeHistoryStatus(status: string): string {
  if (status === 'READY_FOR_PICKUP') return 'READY';
  return status;
}

export function getEstimatedPrepMinutesFromHistory(order: Order): number | null {
  const rows = order.statusHistory ?? [];
  for (const entry of rows) {
    const st = normalizeHistoryStatus(String(entry.status ?? ''));
    if (
      (st === 'CONFIRMED' || st === 'PREPARING') &&
      entry.estimatedPrepMinutes != null &&
      entry.estimatedPrepMinutes > 0
    ) {
      return entry.estimatedPrepMinutes;
    }
  }
  return null;
}

export function getPrepCommitTimestampIso(order: Order): string | null {
  return toIsoTimestamp(order.confirmedAt as string | undefined) ?? toIsoTimestamp(order.preparingAt as string | undefined);
}

/** Context from API/history only (no session). */
export function resolvePrepTimerFromOrder(order: Order): PrepTimerContext | null {
  if (!isPrepTimerActiveStatus(order.status)) return null;
  const duration =
    order.prepTime != null && order.prepTime > 0
      ? order.prepTime
      : getEstimatedPrepMinutesFromHistory(order);
  if (duration == null || duration <= 0) return null;
  const start = getPrepCommitTimestampIso(order);
  if (!start) return null;
  return { startTimeIso: start, durationMinutes: duration };
}

export function mergePrepTimerContext(
  session: PrepTimerContext | null,
  order: Order,
): PrepTimerContext | null {
  if (!isPrepTimerActiveStatus(order.status)) return null;
  if (
    session &&
    session.startTimeIso &&
    session.durationMinutes != null &&
    session.durationMinutes > 0
  ) {
    return session;
  }
  return resolvePrepTimerFromOrder(order);
}

export function buildPrepTimerContextAfterAccept(
  order: Order,
  prepMinutesFromPartner: number,
): PrepTimerContext | null {
  const start =
    getPrepCommitTimestampIso(order) ?? new Date().toISOString();
  const duration =
    prepMinutesFromPartner > 0
      ? prepMinutesFromPartner
      : getEstimatedPrepMinutesFromHistory(order) ?? 0;
  if (duration <= 0) return null;
  return { startTimeIso: start, durationMinutes: duration };
}
