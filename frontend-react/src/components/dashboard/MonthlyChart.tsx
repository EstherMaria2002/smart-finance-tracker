import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { MONTH_LABELS, formatCurrency } from '../../lib/format'
import type { MonthlyReportItem } from '../../types/report'

export function MonthlyChart({ data }: { data: MonthlyReportItem[] }) {
  const rows = data.map((d) => ({ name: MONTH_LABELS[d.month - 1], Ingresos: d.income, Gastos: d.expense }))
  return (
    <div className="chart" role="img" aria-label="Ingresos y gastos por mes">
      <ResponsiveContainer width="100%" height={280}>
        <BarChart data={rows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis dataKey="name" tickLine={false} />
          <YAxis tickLine={false} axisLine={false} width={56} tickFormatter={(v) => `${v} €`} />
          <Tooltip formatter={(value) => formatCurrency(Number(value))} />
          <Legend />
          <Bar dataKey="Ingresos" fill="#1f7a4d" radius={[3, 3, 0, 0]} />
          <Bar dataKey="Gastos" fill="#b8412f" radius={[3, 3, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
