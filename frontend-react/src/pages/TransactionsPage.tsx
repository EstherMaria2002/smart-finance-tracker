import { useMemo, useState } from 'react'
import { getErrorMessage } from '../api/client'
import { TransactionForm } from '../components/transactions/TransactionForm'
import { TransactionTable } from '../components/transactions/TransactionTable'
import { Alert } from '../components/ui/Alert'
import { EmptyState } from '../components/ui/EmptyState'
import { useCategories } from '../hooks/useCategories'
import { useDebounce } from '../hooks/useDebounce'
import {
  useDeleteTransaction,
  useRecategorizeTransaction,
  useTransactions,
  useUpdateTransaction,
} from '../hooks/useTransactions'
import type { TransactionFilters } from '../types/transaction'

export default function TransactionsPage() {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebounce(search.trim().toLowerCase(), 250)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const filters: TransactionFilters = {
    from: from || undefined,
    to: to || undefined,
    categoryId: categoryId ? Number(categoryId) : undefined,
  }

  const categoriesQuery = useCategories()
  const transactionsQuery = useTransactions(filters)
  const updateTransaction = useUpdateTransaction()
  const recategorize = useRecategorizeTransaction()
  const deleteTransaction = useDeleteTransaction()

  const categories = categoriesQuery.data ?? []

  // La búsqueda por texto se hace en el cliente sobre lo ya descargado.
  const visible = useMemo(() => {
    const rows = transactionsQuery.data ?? []
    return debouncedSearch ? rows.filter((t) => t.description.toLowerCase().includes(debouncedSearch)) : rows
  }, [transactionsQuery.data, debouncedSearch])

  function run(id: number, action: (onSettled: () => void, onError: (e: unknown) => void) => void) {
    setBusyId(id)
    setActionError(null)
    action(() => setBusyId(null), (e) => {
      setBusyId(null)
      setActionError(getErrorMessage(e))
    })
  }

  return (
    <>
      <h1>Transacciones</h1>

      <TransactionForm categories={categories} />

      <section className="card" aria-labelledby="tx-list-title">
        <div className="section-head">
          <h2 id="tx-list-title" className="card__title">Historial</h2>
          <div className="filters">
            <input type="search" placeholder="Buscar por descripción" aria-label="Buscar por descripción"
                   value={search} onChange={(e) => setSearch(e.target.value)} />
            <input type="date" aria-label="Desde" value={from} onChange={(e) => setFrom(e.target.value)} />
            <input type="date" aria-label="Hasta" value={to} onChange={(e) => setTo(e.target.value)} />
            <select aria-label="Filtrar por categoría" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
              <option value="">Todas las categorías</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
        </div>

        {actionError && <Alert tone="error">{actionError}</Alert>}
        {transactionsQuery.isLoading && <p>Cargando…</p>}
        {transactionsQuery.isError && <Alert tone="error">{getErrorMessage(transactionsQuery.error)}</Alert>}

        {transactionsQuery.isSuccess && visible.length === 0 && (
          <EmptyState title="No hay transacciones que mostrar">
            Añade tu primera transacción con el formulario de arriba o ajusta los filtros.
          </EmptyState>
        )}

        {visible.length > 0 && (
          <TransactionTable
            transactions={visible}
            categories={categories}
            busyId={busyId}
            onChangeCategory={(id, newCategoryId) =>
              run(id, (done, fail) =>
                updateTransaction.mutate({ id, patch: { categoryId: newCategoryId } }, { onSettled: done, onError: fail }))
            }
            onRecategorize={(id) =>
              run(id, (done, fail) => recategorize.mutate(id, { onSettled: done, onError: fail }))
            }
            onDelete={(id) =>
              run(id, (done, fail) => deleteTransaction.mutate(id, { onSettled: done, onError: fail }))
            }
          />
        )}
      </section>
    </>
  )
}
