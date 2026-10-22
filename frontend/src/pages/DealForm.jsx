import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client.js'
import PriorityButton from '../components/PriorityButton.jsx'
import { PLATFORMS, TRADE_TYPES, addDays, formatDate, nextPriority, todayIso } from '../utils.js'

function emptyForm() {
  return {
    friendName: '',
    trainerCode: '',
    pokemonGive: '',
    pokemonGet: '',
    tradeType: 'IMMEDIATE',
    planDays: '',
    dealDate: todayIso(),
    priority: 'MEDIUM',
    platform: '',
    chatLink: '',
    specialTrade: false,
    notes: '',
  }
}

function fromDeal(deal) {
  return {
    friendName: deal.friendName,
    trainerCode: deal.trainerCode ?? '',
    pokemonGive: deal.pokemonGive,
    pokemonGet: deal.pokemonGet,
    tradeType: deal.tradeType,
    planDays: deal.tradeType === 'CUSTOM' ? String(deal.planDays) : '',
    dealDate: deal.dealDate,
    priority: deal.priority,
    platform: deal.platform ?? '',
    chatLink: deal.chatLink ?? '',
    specialTrade: deal.specialTrade,
    notes: deal.notes ?? '',
  }
}

function toPayload(form) {
  const clean = (v) => (v.trim() === '' ? null : v.trim())
  return {
    friendName: form.friendName.trim(),
    trainerCode: clean(form.trainerCode),
    pokemonGive: form.pokemonGive.trim(),
    pokemonGet: form.pokemonGet.trim(),
    tradeType: form.tradeType,
    planDays: form.tradeType === 'CUSTOM' ? Number(form.planDays) : null,
    dealDate: form.dealDate || null,
    priority: form.priority,
    specialTrade: form.specialTrade,
    platform: clean(form.platform),
    chatLink: clean(form.chatLink),
    notes: clean(form.notes),
  }
}

function validate(form) {
  if (!form.friendName.trim()) return 'Enter the friend’s name.'
  if (!form.pokemonGive.trim()) return 'Enter the Pokémon you give.'
  if (!form.pokemonGet.trim()) return 'Enter the Pokémon you get.'
  if (form.tradeType === 'CUSTOM') {
    const n = Number(form.planDays)
    if (form.planDays === '' || !Number.isInteger(n) || n < 0) return 'Enter the number of days for a custom plan.'
  }
  const link = form.chatLink.trim()
  if (link && !/^https:\/\//i.test(link)) return 'The chat link must start with https://'
  return ''
}

export default function DealForm() {
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(editing)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!editing) return
    setLoading(true)
    api
      .getDeal(id)
      .then((deal) => setForm(fromDeal(deal)))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [editing, id])

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  const days =
    form.tradeType === 'CUSTOM'
      ? form.planDays === ''
        ? null
        : Number(form.planDays)
      : TRADE_TYPES.find((t) => t.value === form.tradeType).days
  const duePreview = days !== null && Number.isFinite(days) && form.dealDate ? addDays(form.dealDate, days) : null

  async function submit(e) {
    e.preventDefault()
    const problem = validate(form)
    if (problem) {
      setError(problem)
      return
    }
    setSaving(true)
    setError('')
    try {
      const payload = toPayload(form)
      if (editing) {
        await api.updateDeal(id, payload)
        navigate('/deals')
      } else {
        await api.createDeal(payload)
        navigate('/')
      }
    } catch (err) {
      setError(err.message)
      setSaving(false)
    }
  }

  async function remove() {
    if (!window.confirm('Delete this deal? This cannot be undone.')) return
    setSaving(true)
    try {
      await api.deleteDeal(id)
      navigate('/deals')
    } catch (err) {
      setError(err.message)
      setSaving(false)
    }
  }

  if (loading) return <p className="loading">Loading deal…</p>

  return (
    <>
      <header className="page__head">
        <h1 className="page__title">{editing ? 'Edit deal' : 'New deal'}</h1>
      </header>

      <form className="form" onSubmit={submit} noValidate>
        <label className="field">
          <span>Friend</span>
          <input value={form.friendName} onChange={set('friendName')} maxLength={100} autoComplete="off" autoFocus={!editing} />
        </label>

        <div className="form__pair">
          <label className="field">
            <span>You give</span>
            <input value={form.pokemonGive} onChange={set('pokemonGive')} maxLength={100} autoComplete="off" />
          </label>
          <label className="field">
            <span>You get</span>
            <input value={form.pokemonGet} onChange={set('pokemonGet')} maxLength={100} autoComplete="off" />
          </label>
        </div>

        <fieldset className="field">
          <legend>When will you trade?</legend>
          <div className="segmented">
            {TRADE_TYPES.map((t) => (
              <button
                key={t.value}
                type="button"
                className={`segmented__btn ${form.tradeType === t.value ? 'segmented__btn--on' : ''}`}
                aria-pressed={form.tradeType === t.value}
                onClick={() => setForm({ ...form, tradeType: t.value })}
              >
                {t.label}
              </button>
            ))}
          </div>
          {form.tradeType === 'CUSTOM' && (
            <label className="field field--tight">
              <span>Days from the deal date</span>
              <input type="number" inputMode="numeric" min="0" max="3650" value={form.planDays} onChange={set('planDays')} />
            </label>
          )}
          {duePreview && <p className="hint">Due {formatDate(duePreview)}</p>}
        </fieldset>

        <div className="field">
          <span>Priority</span>
          <div>
            <PriorityButton value={form.priority} onCycle={() => setForm({ ...form, priority: nextPriority(form.priority) })} />
          </div>
        </div>

        <label className="field">
          <span>Chat link</span>
          <input
            type="url"
            inputMode="url"
            placeholder="https://"
            value={form.chatLink}
            onChange={set('chatLink')}
            maxLength={500}
            autoComplete="off"
          />
        </label>

        <details className="more">
          <summary>More details</summary>
          <div className="more__body">
            <label className="field">
              <span>Platform</span>
              <select value={form.platform} onChange={set('platform')}>
                <option value="">Not set</option>
                {PLATFORMS.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Trainer code</span>
              <input value={form.trainerCode} onChange={set('trainerCode')} maxLength={20} inputMode="numeric" autoComplete="off" />
            </label>
            <label className="field">
              <span>Deal date</span>
              <input type="date" value={form.dealDate} onChange={set('dealDate')} />
            </label>
            <label className="check">
              <input type="checkbox" checked={form.specialTrade} onChange={set('specialTrade')} />
              Uses the daily special trade
            </label>
            <label className="field">
              <span>Notes</span>
              <textarea rows={3} value={form.notes} onChange={set('notes')} maxLength={5000} />
            </label>
          </div>
        </details>

        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        <div className="form__actions">
          <button type="submit" className="btn btn--primary btn--lg" disabled={saving}>
            {saving ? 'Saving…' : editing ? 'Save changes' : 'Save deal'}
          </button>
          {editing && (
            <button type="button" className="btn btn--danger" onClick={remove} disabled={saving}>
              Delete deal
            </button>
          )}
        </div>
      </form>
    </>
  )
}
