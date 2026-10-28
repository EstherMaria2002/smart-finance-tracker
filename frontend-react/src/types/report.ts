export interface MonthlyReportItem {
  /** 1-12 */
  month: number
  income: number
  expense: number
}

export interface CategoryTotal {
  categoryId: number | null
  name: string
  color: string | null
  total: number
}
