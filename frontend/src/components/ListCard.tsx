import type { ReactNode } from 'react'
import Pagination from './Pagination'
import type { PagedList } from '../hooks/usePagedList'

type Props = {
  /** Left side of the header — usually the record count. */
  title: ReactNode
  /** Optional hint under the title. */
  subtitle?: ReactNode
  /** Buttons on the right of the header, e.g. "Làm mới". */
  actions?: ReactNode
  /** The filter bar. Rendered in its own band so every screen puts it in the same place. */
  filters?: ReactNode
  /** Table or grid. */
  children: ReactNode
  /** Supply a paged list to get the pagination footer wired up. */
  list?: Pick<PagedList<unknown>, 'page' | 'size' | 'total' | 'totalPages' | 'setPage' | 'setSize' | 'loading'>
  /** Noun used by the pagination count line. */
  unit?: string
  id?: string
  /** Extra class on the card, for screens that need a different content layout. */
  className?: string
}

/**
 * The single frame every list screen uses: header → filter bar → content → pagination footer.
 *
 * Before this existed the filter bar lived inside `.data-card__header` on some screens and floated
 * loose above the card on others, and there was nowhere consistent to put a pagination row.
 */
export default function ListCard({
  title, subtitle, actions, filters, children, list, unit, id, className,
}: Props) {
  return (
    <section className={className ? `list-card ${className}` : 'list-card'} id={id}>
      <header className="list-card__header">
        <div className="list-card__heading">
          <strong>{title}</strong>
          {subtitle && <span>{subtitle}</span>}
        </div>
        {actions && <div className="list-card__actions">{actions}</div>}
      </header>
      {filters && <div className="list-card__filters">{filters}</div>}
      <div className="list-card__body">{children}</div>
      {list && (
        <footer className="list-card__footer">
          <Pagination
            page={list.page}
            size={list.size}
            total={list.total}
            totalPages={list.totalPages}
            onPage={list.setPage}
            onSize={list.setSize}
            unit={unit}
            loading={list.loading}
          />
        </footer>
      )}
    </section>
  )
}
