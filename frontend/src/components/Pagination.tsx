import { PAGE_SIZES } from '../hooks/usePagedList'

type Props = {
  /** Zero-based, matching the server. */
  page: number
  size: number
  /** Rows matching the current filters, across all pages. */
  total: number
  totalPages: number
  onPage: (page: number) => void
  onSize: (size: number) => void
  /** Noun for the count line, e.g. "bản ghi", "tài khoản". */
  unit?: string
  loading?: boolean
}

/** How many numbered buttons to show around the current page before collapsing to an ellipsis. */
const WINDOW = 1

/**
 * Builds the page-number strip: always the first and last page, a window around the current one,
 * and `null` where a gap was collapsed.
 */
function pageItems(page: number, totalPages: number): Array<number | null> {
  const wanted = new Set<number>([0, totalPages - 1])
  for (let offset = -WINDOW; offset <= WINDOW; offset++) {
    const candidate = page + offset
    if (candidate >= 0 && candidate < totalPages) wanted.add(candidate)
  }
  const sorted = [...wanted].sort((left, right) => left - right)
  const items: Array<number | null> = []
  sorted.forEach((value, index) => {
    if (index > 0 && value - sorted[index - 1] > 1) items.push(null)
    items.push(value)
  })
  return items
}

export default function Pagination({
  page, size, total, totalPages, onPage, onSize, unit = 'bản ghi', loading = false,
}: Props) {
  const pageCount = Math.max(0, totalPages)
  const currentPage = pageCount === 0 ? 0 : Math.min(Math.max(0, page), pageCount - 1)
  const from = total === 0 ? 0 : currentPage * size + 1
  const to = Math.min(total, (currentPage + 1) * size)
  const atStart = currentPage === 0
  const atEnd = pageCount === 0 || currentPage === pageCount - 1
  const navigate = (next: number) => {
    if (loading || pageCount === 0) return
    onPage(Math.min(Math.max(0, next), pageCount - 1))
  }

  return (
    <div className="pagination">
      <div className="pagination__summary" aria-live="polite">
        <p className="pagination__count">
          {total === 0
            ? `Không có ${unit}`
            : <><strong>{from}–{to}</strong><span> trên </span><strong>{total}</strong> {unit}</>}
        </p>
        {pageCount > 0 && <span className="pagination__status">Trang {currentPage + 1} / {pageCount}</span>}
        {loading && <span className="pagination__loading">Đang tải…</span>}
      </div>

      <div className="pagination__controls">
        <label className="pagination__size">
          <span>Hiển thị</span>
          <select
            aria-label="Số dòng mỗi trang"
            value={size}
            disabled={loading}
            onChange={event => onSize(Number(event.target.value))}
          >
            {PAGE_SIZES.map(option => <option key={option} value={option}>{option}</option>)}
          </select>
          <span>dòng</span>
        </label>

        {pageCount > 1 && (
          <nav className="pagination__pages" aria-label="Điều hướng trang">
            <button type="button" className="pagination__step" disabled={loading || atStart} onClick={() => navigate(0)} aria-label="Trang đầu" title="Trang đầu">«</button>
            <button type="button" className="pagination__step" disabled={loading || atStart} onClick={() => navigate(currentPage - 1)} aria-label="Trang trước" title="Trang trước">‹</button>
            <div className="pagination__numbers">
              {pageItems(currentPage, pageCount).map((item, index) => item === null
                ? <span className="pagination__gap" key={`gap-${index}`} aria-hidden="true">…</span>
                : <button
                    type="button"
                    key={item}
                    className={item === currentPage ? 'pagination__page pagination__page--active' : 'pagination__page'}
                    aria-current={item === currentPage ? 'page' : undefined}
                    aria-label={`Trang ${item + 1}`}
                    disabled={loading}
                    onClick={() => navigate(item)}
                  >{item + 1}</button>)}
            </div>
            <button type="button" className="pagination__step" disabled={loading || atEnd} onClick={() => navigate(currentPage + 1)} aria-label="Trang sau" title="Trang sau">›</button>
            <button type="button" className="pagination__step" disabled={loading || atEnd} onClick={() => navigate(pageCount - 1)} aria-label="Trang cuối" title="Trang cuối">»</button>
          </nav>
        )}
      </div>
    </div>
  )
}
