import type { AuthResponse, User } from '../types/user'

const TOKEN_KEY = 'sft.token'
const USER_KEY = 'sft.user'

/** Lee el campo `exp` (en ms) del payload del JWT sin verificar la firma (eso lo hace el backend). */
function readExpiry(token: string): number | null {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    const { exp } = JSON.parse(json) as { exp?: number }
    return typeof exp === 'number' ? exp * 1000 : null
  } catch {
    return null
  }
}

export const tokenStorage = {
  /** Devuelve el token solo si existe y NO ha caducado; si caducó limpia la sesión. */
  getToken(): string | null {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) return null
    const expiry = readExpiry(token)
    if (expiry === null || expiry <= Date.now()) {
      this.clear()
      return null
    }
    return token
  },

  getExpiry(): number | null {
    const token = localStorage.getItem(TOKEN_KEY)
    return token ? readExpiry(token) : null
  },

  getUser(): User | null {
    try {
      const raw = localStorage.getItem(USER_KEY)
      return raw ? (JSON.parse(raw) as User) : null
    } catch {
      return null
    }
  },

  setSession(auth: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, auth.token)
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
  },

  clear(): void {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  },
}
