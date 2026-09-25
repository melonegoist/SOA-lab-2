// Human-readable values. The raw value stays available as a tooltip.

export const NONE = '—';

const numberFormat = new Intl.NumberFormat('en-GB', {maximumFractionDigits: 3});
const dateFormat = new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    timeZoneName: 'short',
});

/** In the browser's time zone, e.g. "10 Sept 2026, 08:19:00 GMT+3". */
export function formatDate(value) {
    if (value === null || value === undefined) {
        return NONE;
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? String(value) : dateFormat.format(date);
}

/** Grouped digits, e.g. "15,320.7"; with a unit when given. */
export function formatNumber(value, unit) {
    if (value === null || value === undefined) {
        return NONE;
    }
    return numberFormat.format(value) + (unit ? ` ${unit}` : '');
}

/** CHOPPER → "Chopper". */
export function formatEnum(value) {
    if (value === null || value === undefined) {
        return NONE;
    }
    return value.charAt(0) + value.slice(1).toLowerCase();
}
