import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { transactionsApi } from '../api/transactions.api'
import type { TransactionFilters, TransactionInput, TransactionUpdate } from '../types/transaction'

/** Una transacción afecta a la lista, a los reportes del dashboard y al gasto de los presupuestos. */
function invalidateFinanceData(queryClient: QueryClient) {
  void queryClient.invalidateQueries({ queryKey: ['transactions'] })
  void queryClient.invalidateQueries({ queryKey: ['reports'] })
  void queryClient.invalidateQueries({ queryKey: ['budgets'] })
}

export function useTransactions(filters: TransactionFilters) {
  return useQuery({
    queryKey: ['transactions', filters],
    queryFn: () => transactionsApi.list(filters),
  })
}

export function useCreateTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: TransactionInput) => transactionsApi.create(input),
    onSuccess: () => invalidateFinanceData(queryClient),
  })
}

export function useUpdateTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, patch }: { id: number; patch: TransactionUpdate }) => transactionsApi.update(id, patch),
    onSuccess: () => invalidateFinanceData(queryClient),
  })
}

export function useRecategorizeTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => transactionsApi.recategorize(id),
    onSuccess: () => invalidateFinanceData(queryClient),
  })
}

export function useDeleteTransaction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => transactionsApi.remove(id),
    onSuccess: () => invalidateFinanceData(queryClient),
  })
}
