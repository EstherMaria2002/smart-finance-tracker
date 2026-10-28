import { formatCurrency, formatDate } from '../../lib/format'
import type { Category } from '../../types/category'
import type { Transaction } from '../../types/transaction'
import { Button } from '../ui/Button'
import { CategoryBadge } from './CategoryBadge'

interface TransactionTableProps {
  transactions: Transaction[]
  categories: Category[]
  /** id de la transacción con una operación en curso (deshabilita sus controles) */
  busyId: number | null
  onChangeCategory: (id: number, categoryId: number) => void
  onRecategorize: (id: number) => void
  onDelete: (id: number) => void
}

export function TransactionTable({
  transactions,
  categories,
  busyId,
  onChangeCategory,
  onRecategorize,
  onDelete,
}: TransactionTableProps) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Descripción</th>
            <th>Categoría</th>
            <th className="num">Importe</th>
            <th>
              <span className="sr-only">Acciones</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((t) => {
            const busy = busyId === t.id
            return (
              <tr key={t.id}>
                <td className="nowrap">{formatDate(t.occurredOn)}</td>
                <td>{t.description}</td>
                <td>
                  <CategoryBadge category={t.category} source={t.categorySource} confidence={t.mlConfidence} />
                </td>
                <td className={`num amount amount--${t.type === 'INCOME' ? 'income' : 'expense'}`}>
                  {t.type === 'INCOME' ? '+' : '−'}
                  {formatCurrency(t.amount)}
                </td>
                <td className="actions">
                  <select
                    aria-label={`Cambiar categoría de ${t.description}`}
                    value=""
                    disabled={busy}
                    onChange={(e) => e.target.value && onChangeCategory(t.id, Number(e.target.value))}
                  >
                    <option value="">Cambiar categoría…</option>
                    {categories
                      .filter((c) => c.id !== t.category?.id)
                      .map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                  </select>
                  <Button variant="secondary" disabled={busy} onClick={() => onRecategorize(t.id)}>
                    Reclasificar
                  </Button>
                  <Button
                    variant="danger"
                    disabled={busy}
                    onClick={() => {
                      if (window.confirm(`¿Eliminar "${t.description}"?`)) onDelete(t.id)
                    }}
                  >
                    Eliminar
                  </Button>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
