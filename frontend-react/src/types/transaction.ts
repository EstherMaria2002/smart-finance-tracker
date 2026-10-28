import type { Category } from './category'

export type TransactionType = 'INCOME' | 'EXPENSE'

/** Quién decidió la categoría: el modelo (ML), el usuario (MANUAL) o las reglas del backend (RULE). */
export type CategorySource = 'ML' | 'MANUAL' | 'RULE'

export interface Transaction {
  id: number
  amount: number
  type: TransactionType
  description: string
  /** YYYY-MM-DD */
  occurredOn: string
  category: Category | null
  categorySource: CategorySource | null
  /** 0..1, solo si categorySource === 'ML' */
  mlConfidence: number | null
  createdAt: string
}

export interface TransactionInput {
  amount: number
  type: TransactionType
  description: string
  occurredOn: string
  /** null => el backend categoriza solo (ML, con reglas si el ML falla) */
  categoryId: number | null
}

export type TransactionUpdate = Partial<TransactionInput>

export interface TransactionFilters {
  from?: string
  to?: string
  categoryId?: number
}
