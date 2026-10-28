import type { CategoryTotal, MonthlyReportItem } from '../types/report'
import { api } from './client'

export const reportsApi = {
  monthly: (year: number) =>
    api.get<MonthlyReportItem[]>('/api/reports/monthly', { params: { year } }).then((r) => r.data),

  byCategory: (from: string, to: string) =>
    api.get<CategoryTotal[]>('/api/reports/by-category', { params: { from, to, type: 'EXPENSE' } }).then((r) => r.data),
}
