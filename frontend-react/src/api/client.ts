import axios, { AxiosError } from 'axios'
import { tokenStorage } from '../auth/tokenStorage'

/** Evento que AuthProvider escucha para cerrar sesión y redirigir a /login. */
export const UNAUTHORIZED_EVENT = 'sft:unauthorized'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

// Inyecta el JWT guardado en localStorage en cada petición.
api.interceptors.request.use((config) => {
  const token = tokenStorage.getToken()
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  }
  return config
})

// 401 en una ruta protegida = token caducado o inválido -> limpiar sesión y mandar a login.
// Se excluye /auth/*: ahí un 401 significa "credenciales incorrectas" y debe mostrarse en el formulario.
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const isAuthCall = error.config?.url?.startsWith('/auth/') ?? false
    if (error.response?.status === 401 && !isAuthCall) {
      tokenStorage.clear()
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)

interface ApiErrorBody {
  message?: string
  fieldErrors?: Record<string, string>
}

/** Extrae un mensaje legible de un error de Axios (formato ApiError del backend). */
export function getErrorMessage(error: unknown, fallback = 'Ha ocurrido un error. Inténtalo de nuevo.'): string {
  if (axios.isAxiosError<ApiErrorBody>(error)) {
    if (!error.response) return 'No se pudo conectar con el servidor.'
    const fields = error.response.data?.fieldErrors
    if (fields && Object.keys(fields).length > 0) return Object.values(fields).join(' · ')
    return error.response.data?.message ?? fallback
  }
  return fallback
}
