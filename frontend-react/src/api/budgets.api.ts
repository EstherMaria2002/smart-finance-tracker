import type { Budget, BudgetInput } from '../types/budget'
import { api } from './client'

export const budgetsApi = {
  /** month: YYYY-MM-DD (cualquier día del mes) */
  list: (month: string) => api.get<Budget[]>('/api/budgets', { params: { month } }).then((r) => r.data),
  upsert: (body: BudgetInput) => api.put<Budget>('/api/budgets', body).then((r) => r.data),
  remove: (id: number) => api.delete<void>(`/api/budgets/${id}`).then(() => undefined),
}
