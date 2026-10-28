import type { ReactNode } from 'react'

interface FieldProps {
  label: string
  htmlFor: string
  hint?: string
  children: ReactNode
}

export function Field({ label, htmlFor, hint, children }: FieldProps) {
  return (
    <div className="field">
      <label htmlFor={htmlFor}>{label}</label>
      {children}
      {hint && <p className="field__hint">{hint}</p>}
    </div>
  )
}
