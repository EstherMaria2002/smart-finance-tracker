import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'

interface LocationState {
  from?: { pathname: string }
}

export default function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const destination = (location.state as LocationState | null)?.from?.pathname ?? '/dashboard'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (user) return <Navigate to={destination} replace />

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(email.trim(), password)
      navigate(destination, { replace: true })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h1>Inicia sesión</h1>
        <Field label="Email" htmlFor="login-email">
          <input id="login-email" type="email" autoComplete="email" value={email}
                 onChange={(e) => setEmail(e.target.value)} required />
        </Field>
        <Field label="Contraseña" htmlFor="login-password">
          <input id="login-password" type="password" autoComplete="current-password" value={password}
                 onChange={(e) => setPassword(e.target.value)} required />
        </Field>
        {error && <Alert tone="error">{error}</Alert>}
        <Button type="submit" loading={loading}>Entrar</Button>
        <p className="auth-card__alt">
          ¿No tienes cuenta? <Link to="/register">Crea una</Link>
        </p>
      </form>
    </div>
  )
}
