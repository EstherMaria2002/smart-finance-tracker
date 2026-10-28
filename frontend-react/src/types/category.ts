export interface Category {
  id: number
  name: string
  color: string | null
  icon: string | null
  system: boolean
}

export interface CategoryInput {
  name: string
  color?: string
  icon?: string
}
