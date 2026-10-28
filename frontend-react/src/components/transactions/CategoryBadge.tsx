import { formatPercent } from '../../lib/format'
import type { Category } from '../../types/category'
import type { CategorySource } from '../../types/transaction'

interface CategoryBadgeProps {
  category: Category | null
  source?: CategorySource | null
  confidence?: number | null
}

const SOURCE_TITLE: Record<CategorySource, string> = {
  ML: 'Categoría sugerida por el modelo de ML',
  RULE: 'Categoría asignada por reglas de palabras clave (el ML no estaba disponible o no estaba seguro)',
  MANUAL: 'Categoría elegida por ti',
}

function sourceLabel(source: CategorySource, confidence?: number | null): string {
  if (source === 'ML') return confidence != null ? `ML ${formatPercent(confidence)}` : 'ML'
  return source === 'RULE' ? 'Regla' : 'Manual'
}

/** Pastilla con el color de la categoría + una etiqueta de quién la decidió (ML / Regla / Manual). */
export function CategoryBadge({ category, source, confidence }: CategoryBadgeProps) {
  if (!category) {
    return <span className="badge badge--none">Sin categoría</span>
  }
  return (
    <span className="badge-group">
      <span className="badge" style={{ borderColor: category.color ?? undefined }}>
        <span className="badge__dot" style={{ background: category.color ?? '#868E96' }} aria-hidden="true" />
        {category.name}
      </span>
      {source && (
        <span className={`source source--${source.toLowerCase()}`} title={SOURCE_TITLE[source]}>
          {sourceLabel(source, confidence)}
        </span>
      )}
    </span>
  )
}
