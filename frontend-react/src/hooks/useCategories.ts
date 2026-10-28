import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { categoriesApi } from '../api/categories.api'
import type { CategoryInput } from '../types/category'

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.list,
    staleTime: 5 * 60_000, // cambian muy poco
  })
}

export function useCreateCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: CategoryInput) => categoriesApi.create(input),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['categories'] }),
  })
}
