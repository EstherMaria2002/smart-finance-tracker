import type { Category } from './category'

export interface Budget {
  id: number
  category: Category
  /** primer día del mes, YYYY-MM-DD */
  month: string
  limitAmount: number
  /** gasto acumulado de la categoría en ese mes */
  spent: number
}

export interface BudgetInput {
  categoryId: number
  month: string
  limitAmount: number
}
