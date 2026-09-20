export function Loading() {
  return <div className="empty-state">Loading…</div>
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="empty-state error-state">
      <p>{message}</p>
      {onRetry && (
        <button className="btn" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  )
}

export function EmptyState({ children }) {
  return <div className="empty-state">{children}</div>
}
