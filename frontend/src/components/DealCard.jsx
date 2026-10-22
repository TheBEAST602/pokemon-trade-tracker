import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client.js'
import { TRADE_LABEL, formatDate, nextPriority, stubFor } from '../utils.js'
import ChatLinkButton from './ChatLinkButton.jsx'
import PriorityButton from './PriorityButton.jsx'

/**
 * One deal as a ticket: the stub on the left is the countdown.
 * onChange() is called after a status change so the parent can reload.
 */
export default function DealCard({ deal, onChange }) {
  const [view, setView] = useState(deal)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => setView(deal), [deal])

  const stub = stubFor(view)
  const planned = view.status === 'PLANNED'

  async function cyclePriority() {
    const previous = view
    const next = nextPriority(view.priority)
    setView({ ...view, priority: next })
    setError('')
    try {
      setView(await api.updatePriority(view.id, next))
    } catch (err) {
      setView(previous)
      setError(err.message)
    }
  }

  async function changeStatus(status) {
    setBusy(true)
    setError('')
    try {
      await api.updateStatus(view.id, status)
      onChange?.()
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  return (
    <article className={`deal ${planned ? '' : 'deal--closed'}`}>
      <div className={`stub stub--${stub.tone}`} aria-label={`${stub.big} ${stub.small}`}>
        <strong className="stub__big">{stub.big}</strong>
        {stub.small && <span className="stub__small">{stub.small}</span>}
      </div>

      <div className="deal__body">
        <header className="deal__head">
          <h3 className="deal__friend">{view.friendName}</h3>
          <PriorityButton value={view.priority} onCycle={cyclePriority} disabled={busy} />
        </header>

        <p className="swap">
          <span className="swap__pokemon">{view.pokemonGive}</span>
          <span className="swap__for">for</span>
          <span className="swap__pokemon">{view.pokemonGet}</span>
        </p>

        <p className="deal__meta">
          <span>{TRADE_LABEL[view.tradeType]}</span>
          <span>Due {formatDate(view.dueDate)}</span>
          {view.platform && <span>{view.platform}</span>}
          {view.specialTrade && <span className="tag">Special trade</span>}
        </p>

        {view.notes && <p className="deal__notes">{view.notes}</p>}
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <footer className="deal__actions">
          <ChatLinkButton href={view.chatLink} platform={view.platform} />
          {planned ? (
            <>
              <button type="button" className="btn btn--done" onClick={() => changeStatus('DONE')} disabled={busy}>
                Mark done
              </button>
              <button type="button" className="btn" onClick={() => changeStatus('CANCELLED')} disabled={busy}>
                Cancel
              </button>
            </>
          ) : (
            <button type="button" className="btn" onClick={() => changeStatus('PLANNED')} disabled={busy}>
              Reopen
            </button>
          )}
          <Link className="btn btn--ghost" to={`/deals/${view.id}/edit`}>
            Edit
          </Link>
        </footer>
      </div>
    </article>
  )
}
