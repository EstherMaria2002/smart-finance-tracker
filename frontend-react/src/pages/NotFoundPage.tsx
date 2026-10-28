import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="auth-page">
      <div className="card auth-card">
        <h1>Página no encontrada</h1>
        <p>La dirección que buscas no existe.</p>
        <Link to="/dashboard">Volver al resumen</Link>
      </div>
    </div>
  )
}
