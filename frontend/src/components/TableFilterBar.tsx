import type { ReactNode } from 'react'

type TableFilterBarProps = {
  children: ReactNode
}

type FilterFieldProps = {
  label: string
  children: ReactNode
  search?: boolean
}

export function FilterField({ label, children, search = false }: FilterFieldProps) {
  return (
    <label className={search ? 'filter-field filter-field--search' : 'filter-field'}>
      <span className="filter-field__label">{label}</span>
      <span className="filter-field__control">{children}</span>
    </label>
  )
}

/**
 * Row of filter controls. Buttons such as "Làm mới" belong in the surrounding {@link ListCard}
 * header, not in here — that is what keeps every list screen laid out the same way.
 */
export default function TableFilterBar({ children }: TableFilterBarProps) {
  return (
    <section className="filter-bar" aria-label="Bộ lọc dữ liệu">
      {children}
    </section>
  )
}
