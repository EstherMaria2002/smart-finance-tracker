import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { formatCurrency } from '../../lib/format'
import type { CategoryTotal } from '../../types/report'

export function CategoryPie({ data }: { data: CategoryTotal[] }) {
  const total = data.reduce((sum, d) => sum + d.total, 0)
  return (
    <div className="category-pie">
      <div className="chart" role="img" aria-label="Gasto por categoría">
        <ResponsiveContainer width="100%" height={240}>
          <PieChart>
            <Pie data={data} dataKey="total" nameKey="name" innerRadius={60} outerRadius={100} paddingAngle={2}>
              {data.map((d) => (
                <Cell key={d.categoryId ?? d.name} fill={d.color ?? '#868e96'} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => formatCurrency(Number(value))} />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <ul className="legend">
        {data.map((d) => (
          <li key={d.categoryId ?? d.name}>
            <span className="badge__dot" style={{ background: d.color ?? '#868e96' }} aria-hidden="true" />
            <span className="legend__name">{d.name}</span>
            <span className="legend__value">
              {formatCurrency(d.total)} · {total > 0 ? Math.round((d.total / total) * 100) : 0} %
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
