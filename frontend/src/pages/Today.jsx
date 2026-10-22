import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client.js'
import DealCard from '../components/DealCard.jsx'
import { formatDate } from '../utils.js'

function Section({ title, count, deals, onChange }) {
  if (deals.length === 0) return null
  return (
    <section className="section">
      <h2 className="section__title">
        {title} <span className="section__count">{count ?? deals.length}</span>
      </h2>
      <div className="stack">
        {deals.map((deal) => (
          <DealCard key={deal.id} deal={deal} onChange={onChange} />
        ))}
      </div>
    </section>
  )
}

export default function Today() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setError('')
    api
      .getToday()
      .then(setData)
      .catch((err) => setError(err.message))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  if (error) {
    return (
      <div className="notice notice--error" role="alert">
        <p>{error}</p>
        <button type="button" className="btn btn--primary" onClick={load}>
          Try again
        </button>
      </div>
    )
  }
  if (!data) return <p className="loading">Loading your deals…</p>

  const nothingDue = data.overdue.length + data.dueToday.length + data.upcoming.length === 0

  return (
    <>
      <header className="page__head">
        <h1 className="page__title">Today</h1>
        <p className="page__sub">{formatDate(data.today)}</p>
      </header>

      <div className={`special ${data.specialTradeUsedToday ? 'special--used' : ''}`}>
        <strong>{data.specialTradeUsedToday ? 'Special trade used' : 'Special trade available'}</strong>
        <span>
          {data.specialTradeUsedToday
            ? 'Today’s one special trade is done.'
            : 'You have not completed a special trade today.'}
        </span>
      </div>

      <Section title="Overdue" deals={data.overdue} onChange={load} />
      <Section title="Due today" deals={data.dueToday} onChange={load} />
      <Section title="Next 7 days" deals={data.upcoming} onChange={load} />

      {nothingDue && (
        <div className="empty">
          <p>
            {data.plannedCount > 0
              ? `Nothing is due in the next 7 days. ${data.plannedCount} planned ${
                  data.plannedCount === 1 ? 'deal is' : 'deals are'
                } further out.`
              : 'No planned deals yet.'}
          </p>
          <Link className="btn btn--primary" to="/deals/new">
            Add a deal
          </Link>
        </div>
      )}
    </>
  )
}
