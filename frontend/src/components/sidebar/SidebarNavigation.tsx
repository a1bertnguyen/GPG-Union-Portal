import { useState } from 'react'
import SidebarNavGroup from './SidebarNavGroup'
import { navGroups, type PageKey } from './navigation'

type SidebarNavigationProps = {
  active: PageKey
  isAdmin: boolean
  onSelect: (key: PageKey) => void
}

export default function SidebarNavigation({ active, isAdmin, onSelect }: SidebarNavigationProps) {
  const [expandedKey, setExpandedKey] = useState<PageKey | null>(null)
  const visibleGroups = navGroups
    .map(group => ({
      ...group,
      items: group.items
        .filter(item => (!item.adminOnly || isAdmin) && (!item.userOnly || !isAdmin))
        .map(item => ({
          ...item,
          children: item.children?.filter(child => (!child.adminOnly || isAdmin) && (!child.userOnly || !isAdmin)),
        })),
    }))
    .filter(group => group.items.length > 0)

  return (
    <nav>
      {visibleGroups.map(group => (
        <SidebarNavGroup
          active={active}
          expandedKey={expandedKey}
          group={group}
          key={group.label}
          onSelect={onSelect}
          onToggle={setExpandedKey}
        />
      ))}
    </nav>
  )
}
