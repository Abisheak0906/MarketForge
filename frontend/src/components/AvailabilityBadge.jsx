import { AVAILABILITY } from '../availability.js'

export default function AvailabilityBadge({ code }) {
  const info = AVAILABILITY[code] || AVAILABILITY.READY
  return <span className={`badge badge-${info.tone}`}>{info.label}</span>
}
