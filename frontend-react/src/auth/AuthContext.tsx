import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth.api'
import { UNAUTHORIZED_EVENT } from '../api/client'
import type { User } from '../types/user'
import { tokenStorage } from './tokenStorage'

interface AuthContextValue {
  user: User | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName?: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

// setTimeout no admite retardos > 2^31-1 ms (~24 días).
const MAX_TIMEOUT_MS = 2_147_483_647

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  // Si el token guardado ya caducó, getToken() lo limpia y arrancamos sin sesión.
  const [user, setUser] = useState<User | null>(() => (tokenStorage.getToken() ? tokenStorage.getUser() : null))

  const logout = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
    queryClient.clear() // no dejar datos del usuario anterior en la caché
  }, [queryClient])

  // El interceptor de Axios avisa con este evento cuando el backend responde 401 (token caducado/inválido).
  // Al quedar user = null, RequireAuth redirige a /login.
  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, logout)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, logout)
  }, [logout])

  // Cierra la sesión justo cuando el JWT caduca, sin esperar a que falle una petición.
  useEffect(() => {
    if (!user) return
    const expiry = tokenStorage.getExpiry()
    if (expiry === null) return
    const timer = window.setTimeout(logout, Math.min(Math.max(expiry - Date.now(), 0), MAX_TIMEOUT_MS))
    return () => window.clearTimeout(timer)
  }, [user, logout])

  const login = useCallback(async (email: string, password: string) => {
    const auth = await authApi.login({ email, password })
    tokenStorage.setSession(auth)
    setUser(auth.user)
  }, [])

  const register = useCallback(async (email: string, password: string, displayName?: string) => {
    const auth = await authApi.register({ email, password, displayName })
    tokenStorage.setSession(auth)
    setUser(auth.user)
  }, [])

  const value = useMemo(() => ({ user, login, register, logout }), [user, login, register, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
