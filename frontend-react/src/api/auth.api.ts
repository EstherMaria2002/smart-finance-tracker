import type { AuthResponse } from '../types/user'
import { api } from './client'

export const authApi = {
  login: (body: { email: string; password: string }) =>
    api.post<AuthResponse>('/auth/login', body).then((r) => r.data),

  register: (body: { email: string; password: string; displayName?: string }) =>
    api.post<AuthResponse>('/auth/register', body).then((r) => r.data),
}
