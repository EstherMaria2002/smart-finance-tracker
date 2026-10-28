import { useState, type FormEvent } from 'react'
import { getErrorMessage } from '../api/client'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { Field } from '../components/ui/Field'
import { useBudgets, useDeleteBudget, useUpsertBudget } from '../hooks/useBudgets'
import { useCategories } from '../hooks/useCategories'
import { currentMonthValue, formatCurrency, monthStart } from '../lib/format'

export default function BudgetsPage() {
  const [month, setMonth] = useState(currentMonthValue())
  const [categoryId, setCategoryId] = useState('')
  const [limit, setLimit] = useState('')
  const [error, setError] = useState<string | null>(null)

  const monthDate = monthStart(month)
  const categories = useCategories().data ?? []
  const budgetsQuery = useBudgets(monthDate)
  const upsert = useUpsertBudget()
  const remove = useDeleteBudget()

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const parsed = Number(limit.replace(',', '.'))
    if (!categoryId) return setError('Elige una categoría.')
    if (!Number.isFinite(parsed) || parsed < 0) return setError('Introduce un límite válido.')

    upsert.mutate(
      { categoryId: Number(categoryId), month: monthDate, limitAmount: parsed },
      {
        onSuccess: () => {
          setLimit('')
          setCategoryId('')
        },
        onError: (e) => setError(getErrorMessage(e)),
      },
    )
  }

  return (
    <>
      <div className="section-head">
        <h1>Presupuestos</h1>
        <input type="month" aria-label="Mes" value={month} onChange={(e) => e.target.value && setMonth(e.target.value)} />
      </div>

      <form className="card form-grid" onSubmit={handleSubmit} noValidate>
        <h2 className="card__title">Fijar límite mensual</h2>
        <Field label="Categoría" htmlFor="budget-category">
          <select id="budget-category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">Elige una categoría</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </Field>
        <Field label="Límite (€)" htmlFor="budget-limit" hint="Si ya existe un límite para esa categoría y mes, se actualiza.">
          <input id="budget-limit" inputMode="decimal" value={limit} onChange={(e) => setLimit(e.target.value)} placeholder="300" />
        </Field>
        <div className="form-grid__actions">
          <Button type="submit" loading={upsert.isPending}>Guardar límite</Button>
        </div>
        {error && <Alert tone="error">{error}</Alert>}
      </form>

      <section className="card" aria-labelledby="budget-list-title">
        <h2 id="budget-list-title" className="card__title">Estado del mes</h2>
        {budgetsQuery.isLoading && <p>Cargando…</p>}
        {budgetsQuery.isError && <Alert tone="error">{getErrorMessage(budgetsQuery.error)}</Alert>}
        {budgetsQuery.isSuccess && budgetsQuery.data.length === 0 && (
          <EmptyState title="Aún no has fijado presupuestos para este mes">
            Elige una categoría y un límite arriba para empezar a controlar tu gasto.
          </EmptyState>
        )}

        <ul className="budget-list">
          {(budgetsQuery.data ?? []).map((b) => {
            const ratio = b.limitAmount > 0 ? b.spent / b.limitAmount : b.spent > 0 ? 1 : 0
            const state = ratio > 1 ? 'over' : ratio >= 0.8 ? 'warn' : 'ok'
            return (
              <li key={b.id} className="budget">
                <div className="budget__head">
                  <span className="badge">
                    <span className="badge__dot" style={{ background: b.category.color ?? '#868e96' }} aria-hidden="true" />
                    {b.category.name}
                  </span>
                  <span className="budget__figures">
                    {formatCurrency(b.spent)} de {formatCurrency(b.limitAmount)}
                  </span>
                  <Button variant="ghost" disabled={remove.isPending} onClick={() => remove.mutate(b.id)}>
                    Quitar
                  </Button>
                </div>
                <div
                  className="progress"
                  role="progressbar"
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-valuenow={Math.min(Math.round(ratio * 100), 100)}
                  aria-label={`Presupuesto de ${b.category.name}`}
                >
                  <div className={`progress__bar progress__bar--${state}`} style={{ width: `${Math.min(ratio, 1) * 100}%` }} />
                </div>
                <p className={`budget__status budget__status--${state}`}>
                  {state === 'over'
                    ? `Te has pasado ${formatCurrency(b.spent - b.limitAmount)}`
                    : `Te quedan ${formatCurrency(b.limitAmount - b.spent)}`}
                </p>
              </li>
            )
          })}
        </ul>
      </section>
    </>
  )
}
