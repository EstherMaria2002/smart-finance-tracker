import { useMemo, useState } from 'react'
import { getErrorMessage } from '../api/client'
import { CategoryPie } from '../components/dashboard/CategoryPie'
import { KpiCard } from '../components/dashboard/KpiCard'
import { MonthlyChart } from '../components/dashboard/MonthlyChart'
import { Alert } from '../components/ui/Alert'
import { EmptyState } from '../components/ui/EmptyState'
import { useBudgets } from '../hooks/useBudgets'
import { useCategoryReport, useMonthlyReport } from '../hooks/useReports'
import { currentMonthValue, formatCurrency, formatPercent, monthRange, monthStart } from '../lib/format'

export default function DashboardPage() {
  const [month, setMonth] = useState(currentMonthValue())
  const year = Number(month.slice(0, 4))
  const monthIndex = Number(month.slice(5, 7)) - 1
  const { from, to } = monthRange(month)

  const monthly = useMonthlyReport(year)
  const byCategory = useCategoryReport(from, to)
  const budgets = useBudgets(monthStart(month))

  const current = monthly.data?.[monthIndex]
  const income = current?.income ?? 0
  const expense = current?.expense ?? 0
  const balance = income - expense
  const savingsRate = income > 0 ? balance / income : null

  // Presupuestos que ya superaron el 80 % de su límite este mes.
  const budgetsAtRisk = useMemo(
    () => (budgets.data ?? []).filter((b) => b.limitAmount > 0 && b.spent / b.limitAmount >= 0.8).length,
    [budgets.data],
  )

  const error = monthly.error ?? byCategory.error

  return (
    <>
      <div className="section-head">
        <h1>Resumen</h1>
        <input type="month" aria-label="Mes" value={month} onChange={(e) => e.target.value && setMonth(e.target.value)} />
      </div>

      {error && <Alert tone="error">{getErrorMessage(error)}</Alert>}

      <div className="kpis">
        <KpiCard label="Ingresos" value={formatCurrency(income)} tone="income" />
        <KpiCard label="Gastos" value={formatCurrency(expense)} tone="expense" />
        <KpiCard label="Balance" value={formatCurrency(balance)} tone={balance >= 0 ? 'income' : 'expense'} />
        <KpiCard
          label="Ahorro"
          value={savingsRate === null ? '—' : formatPercent(savingsRate)}
          note={budgetsAtRisk > 0 ? `${budgetsAtRisk} presupuesto(s) al 80 % o más` : undefined}
        />
      </div>

      <div className="dashboard-grid">
        <section className="card" aria-labelledby="monthly-title">
          <h2 id="monthly-title" className="card__title">Ingresos y gastos de {year}</h2>
          {monthly.isLoading ? <p>Cargando…</p> : <MonthlyChart data={monthly.data ?? []} />}
        </section>

        <section className="card" aria-labelledby="category-title">
          <h2 id="category-title" className="card__title">Gasto por categoría</h2>
          {byCategory.isLoading && <p>Cargando…</p>}
          {byCategory.isSuccess && byCategory.data.length === 0 && (
            <EmptyState title="Sin gastos este mes">Cuando registres gastos aparecerán aquí por categoría.</EmptyState>
          )}
          {byCategory.isSuccess && byCategory.data.length > 0 && <CategoryPie data={byCategory.data} />}
        </section>
      </div>
    </>
  )
}
