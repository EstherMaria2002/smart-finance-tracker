import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { Field } from '../components/ui/Field'

export default function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()

  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  if (user) return <Navigate to="/dashboard" replace />

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (password.length < 8) {
      setError('La contraseña debe tener al menos 8 caracteres.')
      return
    }
    setLoading(true)
    try {
      await register(email.trim(), password, displayName.trim() || undefined)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h1>Crea tu cuenta</h1>
        <Field label="Nombre (opcional)" htmlFor="reg-name">
          <input id="reg-name" autoComplete="name" value={displayName}
                 onChange={(e) => setDisplayName(e.target.value)} maxLength={100} />
        </Field>
        <Field label="Email" htmlFor="reg-email">
          <input id="reg-email" type="email" autoComplete="email" value={email}
                 onChange={(e) => setEmail(e.target.value)} required />
        </Field>
        <Field label="Contraseña" htmlFor="reg-password" hint="Mínimo 8 caracteres.">
          <input id="reg-password" type="password" autoComplete="new-password" value={password}
                 onChange={(e) => setPassword(e.target.value)} required />
        </Field>
        {error && <Alert tone="error">{error}</Alert>}
        <Button type="submit" loading={loading}>Crear cuenta</Button>
        <p className="auth-card__alt">
          ¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link>
        </p>
      </form>
    </div>
  )
}
