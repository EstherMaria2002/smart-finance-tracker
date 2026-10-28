export interface User {
  id: number
  email: string
  displayName: string | null
}

export interface AuthResponse {
  token: string
  tokenType: string
  /** segundos hasta que caduca el token */
  expiresIn: number
  user: User
}
