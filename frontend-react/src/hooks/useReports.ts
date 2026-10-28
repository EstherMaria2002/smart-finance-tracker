import { useQuery } from '@tanstack/react-query'
import { reportsApi } from '../api/reports.api'

export function useMonthlyReport(year: number) {
  return useQuery({
    queryKey: ['reports', 'monthly', year],
    queryFn: () => reportsApi.monthly(year),
  })
}

export function useCategoryReport(from: string, to: string) {
  return useQuery({
    queryKey: ['reports', 'by-category', from, to],
    queryFn: () => reportsApi.byCategory(from, to),
  })
}
