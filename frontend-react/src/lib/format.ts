const currency = new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' })
const percent = new Intl.NumberFormat('es-ES', { style: 'percent', maximumFractionDigits: 0 })

export const formatCurrency = (value: number): string => currency.format(value)

export const formatPercent = (ratio: number): string => percent.format(ratio)

/** "2026-09-20" -> "20 sept 2026" (sin pasar por Date/UTC para no desplazar el día). */
export function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' })
}

const pad = (n: number): string => String(n).padStart(2, '0')

/** Fecha local de hoy como YYYY-MM-DD (toISOString daría la fecha UTC, que puede ser ayer/mañana). */
export function todayISO(): string {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** Mes actual como YYYY-MM (valor de <input type="month">). */
export function currentMonthValue(): string {
  return todayISO().slice(0, 7)
}

/** "2026-09" -> "2026-09-01" */
export function monthStart(month: string): string {
  return `${month}-01`
}

/** "2026-09" -> { from: "2026-09-01", to: "2026-09-30" } */
export function monthRange(month: string): { from: string; to: string } {
  const [y, m] = month.split('-').map(Number)
  const lastDay = new Date(y, m, 0).getDate()
  return { from: `${month}-01`, to: `${month}-${pad(lastDay)}` }
}

export const MONTH_LABELS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
