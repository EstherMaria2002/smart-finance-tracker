interface KpiCardProps {
  label: string
  value: string
  tone?: 'income' | 'expense' | 'neutral'
  note?: string
}

export function KpiCard({ label, value, tone = 'neutral', note }: KpiCardProps) {
  return (
    <div className="card kpi">
      <p className="kpi__label">{label}</p>
      <p className={`kpi__value kpi__value--${tone}`}>{value}</p>
      {note && <p className="kpi__note">{note}</p>}
    </div>
  )
}
