/** Opens the original chat in a new tab. Backend only stores https links. */
export default function ChatLinkButton({ href, platform }) {
  if (!href) {
    return (
      <span className="btn btn--ghost btn--disabled" aria-disabled="true">
        No chat link
      </span>
    )
  }
  return (
    <a className="btn btn--primary" href={href} target="_blank" rel="noopener noreferrer">
      Open {platform || 'chat'}
    </a>
  )
}
