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

function resolvePrepDurationMinutes(order: Order): number | null {
  const fromOrder = order.prepTime != null && order.prepTime > 0 ? order.prepTime : null;
  const fromHistory = getEstimatedPrepMinutesFromHistory(order);
  const d = fromOrder ?? fromHistory;
  return d != null && d > 0 ? d : null;
}

/**
 * Commande planifiée : le compteur arrive à 0 à
 * {@code scheduledDeliveryTime - prepMinutes} (fin de la fenêtre de prépa avant le créneau client).
 * Fenêtre affichée : [deadline - prep, deadline] (même durée en minutes que le temps annoncé).
 */
export function computeScheduledPrepTimerContext(
  scheduledDeliveryIso: string,
  prepMinutes: number,
): PrepTimerContext | null {
  if (prepMinutes <= 0) return null;
  const schedMs = Date.parse(scheduledDeliveryIso);
  if (Number.isNaN(schedMs)) return null;

  const deadlineMs = schedMs - prepMinutes * 60_000;
  const nowMs = Date.now();

  // Phase 1 (waiting): countdown until prep should start.
  if (nowMs < deadlineMs) {
    return {
      startTimeIso: new Date(nowMs).toISOString(),
      durationMinutes: Math.max(1, Math.ceil((deadlineMs - nowMs) / 60_000)),
    };
  }

  // Phase 2 (prep): once due time is reached, run prep countdown.
  return {
    startTimeIso: new Date(deadlineMs).toISOString(),
    durationMinutes: prepMinutes,
  };
}

/** Context from API/history only (no session). */
export function resolvePrepTimerFromOrder(order: Order): PrepTimerContext | null {
  if (!isPrepTimerActiveStatus(order.status)) return null;
  const duration = resolvePrepDurationMinutes(order);
  if (duration == null) return null;

  if (order.isScheduled === true && order.scheduledDeliveryTime) {
    return computeScheduledPrepTimerContext(order.scheduledDeliveryTime, duration);
  }

  const start = getPrepCommitTimestampIso(order);
  if (!start) return null;
  return { startTimeIso: start, durationMinutes: duration };
}

export function mergePrepTimerContext(
  session: PrepTimerContext | null,
  order: Order,
): PrepTimerContext | null {
  if (!isPrepTimerActiveStatus(order.status)) return null;

  if (order.isScheduled === true && order.scheduledDeliveryTime) {
    const durationFromSession =
      session && session.durationMinutes != null && session.durationMinutes > 0
        ? session.durationMinutes
        : null;
    const duration = durationFromSession ?? resolvePrepDurationMinutes(order);
    if (duration == null || duration <= 0) return null;
    return computeScheduledPrepTimerContext(order.scheduledDeliveryTime, duration);
  }

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
  const duration =
    prepMinutesFromPartner > 0
      ? prepMinutesFromPartner
      : getEstimatedPrepMinutesFromHistory(order) ?? 0;
  if (duration <= 0) return null;

  if (order.isScheduled === true && order.scheduledDeliveryTime) {
    const ctx = computeScheduledPrepTimerContext(order.scheduledDeliveryTime, duration);
    if (ctx) return ctx;
  }

  const start =
    getPrepCommitTimestampIso(order) ?? new Date().toISOString();
  return { startTimeIso: start, durationMinutes: duration };
}
