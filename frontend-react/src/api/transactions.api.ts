import type { Transaction, TransactionFilters, TransactionInput, TransactionUpdate } from '../types/transaction'
import { api } from './client'

export const transactionsApi = {
  list: (filters: TransactionFilters = {}) =>
    api.get<Transaction[]>('/api/transactions', { params: filters }).then((r) => r.data),

  create: (body: TransactionInput) => api.post<Transaction>('/api/transactions', body).then((r) => r.data),

  update: (id: number, body: TransactionUpdate) =>
    api.patch<Transaction>(`/api/transactions/${id}`, body).then((r) => r.data),

  remove: (id: number) => api.delete<void>(`/api/transactions/${id}`).then(() => undefined),

  recategorize: (id: number) =>
    api.post<Transaction>(`/api/transactions/${id}/recategorize`).then((r) => r.data),
}
