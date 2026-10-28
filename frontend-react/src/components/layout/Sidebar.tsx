import { NavLink } from 'react-router-dom'

const links = [
  { to: '/dashboard', label: 'Resumen' },
  { to: '/transactions', label: 'Transacciones' },
  { to: '/budgets', label: 'Presupuestos' },
]

export function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar__brand">Smart Finance</div>
      <nav className="sidebar__nav" aria-label="Principal">
        {links.map((link) => (
          <NavLink key={link.to} to={link.to} className={({ isActive }) => (isActive ? 'active' : undefined)}>
            {link.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
