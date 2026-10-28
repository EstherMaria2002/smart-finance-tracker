import { useAuth } from '../../auth/AuthContext'
import { Button } from '../ui/Button'

export function Topbar() {
  const { user, logout } = useAuth()
  return (
    <header className="topbar">
      <span className="topbar__user">{user?.displayName || user?.email}</span>
      <Button variant="ghost" onClick={logout}>
        Cerrar sesión
      </Button>
    </header>
  )
}
