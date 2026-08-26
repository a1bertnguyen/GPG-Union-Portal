export function getCanonicalUrl(canonicalOrigin: string | undefined, currentHref: string) {
  if (!canonicalOrigin) return null

  try {
    const canonicalUrl = new URL(canonicalOrigin)
    const currentUrl = new URL(currentHref)

    if (canonicalUrl.origin === currentUrl.origin) return null

    canonicalUrl.pathname = currentUrl.pathname
    canonicalUrl.search = currentUrl.search
    canonicalUrl.hash = currentUrl.hash
    return canonicalUrl.toString()
  } catch {
    return null
  }
}
