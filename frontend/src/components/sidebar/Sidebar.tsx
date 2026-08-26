import SidebarNavigation from './SidebarNavigation'
import type { PageKey } from './navigation'

type SidebarProps = {
  active: PageKey
  isAdmin: boolean
  isOpen: boolean
  unitName?: string
  onSelect: (key: PageKey) => void
}

export default function Sidebar({ active, isAdmin, isOpen, unitName, onSelect }: SidebarProps) {
  return (
    <aside className={isOpen ? 'sidebar sidebar--open' : 'sidebar'}>
      <SidebarNavigation active={active} isAdmin={isAdmin} onSelect={onSelect} />
      <div className="sidebar__footer">
        <span className="system-dot" />
        Hệ thống nội bộ
        <span>{isAdmin ? 'Toàn hệ thống' : unitName}</span>
      </div>
    </aside>
  )
}
