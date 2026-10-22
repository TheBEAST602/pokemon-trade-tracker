import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client.js'
import DealCard from '../components/DealCard.jsx'
import FilterBar from '../components/FilterBar.jsx'

const DEFAULT_FILTERS = { status: 'PLANNED', priority: '', tradeType: '', platform: '', overdue: false, sort: '' }

export default function Deals() {
  const [filters, setFilters] = useState(DEFAULT_FILTERS)
  const [deals, setDeals] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setError('')
    api
      .listDeals(filters)
      .then(setDeals)
      .catch((err) => setError(err.message))
  }, [filters])

  useEffect(() => {
    load()
  }, [load])

  return (
    <>
      <header className="page__head">
        <h1 className="page__title">All deals</h1>
        <p className="page__sub">{deals ? `${deals.length} shown` : ' '}</p>
      </header>

      <FilterBar filters={filters} onChange={setFilters} />

      {error && (
        <div className="notice notice--error" role="alert">
          <p>{error}</p>
          <button type="button" className="btn btn--primary" onClick={load}>
            Try again
          </button>
        </div>
      )}
      {!error && !deals && <p className="loading">Loading deals…</p>}
      {!error && deals && deals.length === 0 && (
        <div className="empty">
          <p>No deals match these filters.</p>
          <Link className="btn btn--primary" to="/deals/new">
            Add a deal
          </Link>
        </div>
      )}
      <div className="stack">
        {!error && deals?.map((deal) => <DealCard key={deal.id} deal={deal} onChange={load} />)}
      </div>
    </>
  )
}
