import type { OrderItem, OrderItemAddon, OrderItemOption } from '../models/order.model';

/** Lignes secondaires (options, suppléments, note produit) pour liste / carte. */
export function partnerOrderItemMetaLines(item: OrderItem): string[] {
  const lines: string[] = [];
  const priceTxt = (p: number) => (p > 0 ? ` (+${p.toFixed(2)} TND)` : '');

  for (const o of item.options ?? []) {
    const pr = Number(o.price ?? 0);
    const name = (o.name ?? '').trim();
    const value = (o.value ?? '').trim();
    if (name && value && name !== value) {
      lines.push(`${name}: ${value}${priceTxt(pr)}`);
    } else if (name || value) {
      lines.push(`${name || value}${priceTxt(pr)}`);
    }
  }

  for (const a of item.addons ?? []) {
    const q = a.quantity ?? 1;
    const tot =
      a.total != null && Number(a.total) > 0
        ? Number(a.total)
        : Number(a.price ?? 0) * q;
    lines.push(`${a.addonName} ×${q}${priceTxt(tot)}`);
  }

  const n = item.notes?.trim();
  if (n) lines.push(`📝 ${n}`);
  return lines;
}

export function partnerOrderItemsLines(order: {
  items?: OrderItem[];
}): { title: string; meta: string[] }[] {
  return (order.items ?? []).map((it) => ({
    title: `${it.quantity ?? 1}× ${it.productName ?? ''}`.trim(),
    meta: partnerOrderItemMetaLines(it),
  }));
}

export function normalizePartnerOrderItem(it: any): OrderItem {
  const productId = it.productId != null ? String(it.productId) : it.productId;
  const unitPrice = Number(it.unitPrice ?? it.price ?? 0);
  const qty = it.quantity != null ? Number(it.quantity) : 1;
  const subtotal =
    it.subtotal != null && Number(it.subtotal) >= 0 ? Number(it.subtotal) : undefined;
  const linePrice = subtotal ?? Number(it.price ?? unitPrice * qty);
  const preparationTimeMin =
    it.preparationTimeMin != null ? Number(it.preparationTimeMin) : undefined;

  const options: OrderItemOption[] = [];
  const seen = new Set<string>();

  const pushOpt = (nameRaw: string, valueRaw: string, price: number) => {
    const name = (nameRaw ?? '').trim();
    const value = (valueRaw ?? '').trim();
    if (!name && !value) return;
    const key = `${name}|${value}|${price}`;
    if (seen.has(key)) return;
    seen.add(key);
    options.push({
      name: name || value,
      value: name && value && name !== value ? value : '',
      price: Number.isFinite(price) ? price : 0,
    });
  };

  for (const opt of it.selectedOptions ?? []) {
    const name = String(
      opt.optionName ??
        opt.groupName ??
        opt.groupLabel ??
        opt.name ??
        '',
    );
    const value = String(
      opt.valueName ??
        opt.optionValueName ??
        opt.label ??
        opt.value ??
        '',
    );
    const price = Number(opt.priceModifier ?? opt.price ?? 0);
    pushOpt(name, value, price);
  }

  for (const lo of it.options ?? []) {
    if (!lo) continue;
    pushOpt(String(lo.name ?? ''), String(lo.value ?? ''), Number(lo.price ?? 0));
  }

  const addons: OrderItemAddon[] = [];
  for (const ad of it.selectedAddons ?? []) {
    const addonName = String(ad.addonName ?? '').trim();
    if (!addonName) continue;
    const quantity = ad.quantity != null ? Math.max(1, Number(ad.quantity)) : 1;
    const price = Number(ad.price ?? 0);
    const total =
      ad.total != null && Number(ad.total) >= 0
        ? Number(ad.total)
        : price * quantity;
    addons.push({ addonName, quantity, price, total });
  }

  const noteRaw = [
    it.specialInstructions,
    it.special_instructions,
    it.notes,
    it.kitchenNote,
    it.kitchen_note,
    it.itemNote,
    it.lineNote,
  ].find((x: unknown) => typeof x === 'string' && String(x).trim().length > 0) as string | undefined;
  const notes = noteRaw?.trim() || undefined;

  return {
    ...it,
    id: it.id != null ? String(it.id) : it.id,
    productId,
    productName: String(it.productName ?? ''),
    quantity: qty,
    unitPrice,
    price: linePrice,
    subtotal,
    preparationTimeMin,
    options: options.length ? options : undefined,
    addons: addons.length ? addons : undefined,
    notes,
  };
}
