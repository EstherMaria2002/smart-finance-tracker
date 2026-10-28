import { useState, type FormEvent } from 'react'
import { getErrorMessage } from '../../api/client'
import { useCreateTransaction } from '../../hooks/useTransactions'
import { todayISO } from '../../lib/format'
import type { Category } from '../../types/category'
import type { Transaction, TransactionType } from '../../types/transaction'
import { Alert } from '../ui/Alert'
import { Button } from '../ui/Button'
import { Field } from '../ui/Field'
import { CategoryBadge } from './CategoryBadge'

interface TransactionFormProps {
  categories: Category[]
}

export function TransactionForm({ categories }: TransactionFormProps) {
  const createTransaction = useCreateTransaction()
  const [type, setType] = useState<TransactionType>('EXPENSE')
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [occurredOn, setOccurredOn] = useState(todayISO())
  // '' = "Automática": se envía categoryId null y el backend categoriza con ML.
  const [categoryId, setCategoryId] = useState('')
  const [formError, setFormError] = useState<string | null>(null)
  const [created, setCreated] = useState<Transaction | null>(null)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)
    setCreated(null)

    const parsedAmount = Number(amount.replace(',', '.'))
    if (!description.trim()) return setFormError('Escribe una descripción.')
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) return setFormError('El importe debe ser mayor que 0.')

    createTransaction.mutate(
      {
        amount: parsedAmount,
        type,
        description: description.trim(),
        occurredOn,
        categoryId: categoryId === '' ? null : Number(categoryId),
      },
      {
        onSuccess: (transaction) => {
          setCreated(transaction)
          setDescription('')
          setAmount('')
        },
        onError: (error) => setFormError(getErrorMessage(error)),
      },
    )
  }

  return (
    <form className="card form-grid" onSubmit={handleSubmit} noValidate>
      <h2 className="card__title">Nueva transacción</h2>

      <Field label="Tipo" htmlFor="tx-type">
        <select id="tx-type" value={type} onChange={(e) => setType(e.target.value as TransactionType)}>
          <option value="EXPENSE">Gasto</option>
          <option value="INCOME">Ingreso</option>
        </select>
      </Field>

      <Field label="Descripción" htmlFor="tx-description">
        <input
          id="tx-description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          maxLength={255}
          placeholder="Cena en restaurante italiano"
          required
        />
      </Field>

      <Field label="Importe (€)" htmlFor="tx-amount">
        <input
          id="tx-amount"
          inputMode="decimal"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="42,50"
          required
        />
      </Field>

      <Field label="Fecha" htmlFor="tx-date">
        <input id="tx-date" type="date" value={occurredOn} onChange={(e) => setOccurredOn(e.target.value)} required />
      </Field>

      <Field
        label="Categoría"
        htmlFor="tx-category"
        hint="Déjala en automática y la categoría se asigna sola a partir de la descripción."
      >
        <select id="tx-category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">Automática</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </Field>

      <div className="form-grid__actions">
        <Button type="submit" loading={createTransaction.isPending}>
          Añadir transacción
        </Button>
      </div>

      {formError && <Alert tone="error">{formError}</Alert>}
      {created && (
        <Alert tone="success">
          Guardada en{' '}
          <CategoryBadge category={created.category} source={created.categorySource} confidence={created.mlConfidence} />
        </Alert>
      )}
    </form>
  )
}
