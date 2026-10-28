import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { budgetsApi } from '../api/budgets.api'
import type { BudgetInput } from '../types/budget'

/** month: YYYY-MM-DD (primer día del mes) */
export function useBudgets(month: string) {
  return useQuery({
    queryKey: ['budgets', month],
    queryFn: () => budgetsApi.list(month),
  })
}

export function useUpsertBudget() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: BudgetInput) => budgetsApi.upsert(input),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['budgets'] }),
  })
}

export function useDeleteBudget() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => budgetsApi.remove(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['budgets'] }),
  })
}
