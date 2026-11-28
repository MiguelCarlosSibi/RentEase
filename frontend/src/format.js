// Small helpers for showing money and dates the same way on every page.
export const peso = (amount) =>
  new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(Number(amount));

// Dates from the API look like 2026-12-31. Adding a time keeps the browser from shifting the day.
export const fmtDate = (iso) =>
  iso
    ? new Date(`${iso}T00:00:00`).toLocaleDateString('en-PH', { year: 'numeric', month: 'short', day: 'numeric' })
    : '';
