import type { Category, CategoryInput } from '../types/category'
import { api } from './client'

export const categoriesApi = {
  list: () => api.get<Category[]>('/api/categories').then((r) => r.data),
  create: (body: CategoryInput) => api.post<Category>('/api/categories', body).then((r) => r.data),
}
