import SidebarNavItem from './SidebarNavItem'
import type { PageKey, SidebarNavGroup as SidebarNavGroupModel } from './navigation'

type SidebarNavGroupProps = {
  group: SidebarNavGroupModel
  active: PageKey
  expandedKey: PageKey | null
  onSelect: (key: PageKey) => void
  onToggle: (key: PageKey | null) => void
}

export default function SidebarNavGroup({ group, active, expandedKey, onSelect, onToggle }: SidebarNavGroupProps) {
  return (
    <div className="nav-group">
      <p>{group.label}</p>
      {group.items.map(item => (
        <SidebarNavItem
          active={active === item.key || Boolean(item.children?.some(child => child.key === active))}
          activeKey={active}
          expanded={expandedKey === item.key}
          item={item}
          key={item.key}
          onSelect={onSelect}
          onToggle={onToggle}
        />
      ))}
    </div>
  )
}
