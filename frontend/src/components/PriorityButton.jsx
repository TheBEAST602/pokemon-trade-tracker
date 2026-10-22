import { PRIORITY_LABEL } from '../utils.js'

/** Tap to cycle High, Medium, Low. */
export default function PriorityButton({ value, onCycle, disabled }) {
  return (
    <button
      type="button"
      className={`priority priority--${value.toLowerCase()}`}
      onClick={onCycle}
      disabled={disabled}
      aria-label={`Priority ${PRIORITY_LABEL[value]}. Tap to change.`}
    >
      <span className="priority__dot" aria-hidden="true" />
      {PRIORITY_LABEL[value]}
    </button>
  )
}
