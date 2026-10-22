import { PLATFORMS, PRIORITIES, PRIORITY_LABEL, STATUS_LABEL, TRADE_TYPES } from '../utils.js'

function Chips({ label, options, value, onSelect }) {
  return (
    <div className="chips" role="group" aria-label={label}>
      <span className="chips__label">{label}</span>
      {options.map((opt) => (
        <button
          key={opt.value}
          type="button"
          className={`chip ${value === opt.value ? 'chip--on' : ''}`}
          aria-pressed={value === opt.value}
          onClick={() => onSelect(opt.value)}
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}

export default function FilterBar({ filters, onChange }) {
  const set = (key) => (value) => onChange({ ...filters, [key]: value })

  return (
    <section className="filters" aria-label="Filters">
      <Chips
        label="Status"
        value={filters.status}
        onSelect={set('status')}
        options={[
          { value: 'PLANNED', label: STATUS_LABEL.PLANNED },
          { value: 'DONE', label: STATUS_LABEL.DONE },
          { value: 'CANCELLED', label: STATUS_LABEL.CANCELLED },
          { value: '', label: 'All' },
        ]}
      />
      <Chips
        label="Priority"
        value={filters.priority}
        onSelect={set('priority')}
        options={[{ value: '', label: 'Any' }, ...PRIORITIES.map((p) => ({ value: p, label: PRIORITY_LABEL[p] }))]}
      />
      <Chips
        label="Type"
        value={filters.tradeType}
        onSelect={set('tradeType')}
        options={[{ value: '', label: 'Any' }, ...TRADE_TYPES.map((t) => ({ value: t.value, label: t.label }))]}
      />

      <div className="filters__row">
        <label className="field field--inline">
          <span>Platform</span>
          <select value={filters.platform} onChange={(e) => set('platform')(e.target.value)}>
            <option value="">Any</option>
            {PLATFORMS.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        </label>
        <label className="field field--inline">
          <span>Sort by</span>
          <select value={filters.sort} onChange={(e) => set('sort')(e.target.value)}>
            <option value="">Overdue, priority, date</option>
            <option value="dueDate">Due date</option>
            <option value="priority">Priority</option>
          </select>
        </label>
        <label className="check">
          <input type="checkbox" checked={filters.overdue} onChange={(e) => set('overdue')(e.target.checked)} />
          Overdue only
        </label>
      </div>
    </section>
  )
}
