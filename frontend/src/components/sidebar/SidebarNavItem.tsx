import type { PageKey, SidebarNavItem as SidebarNavItemModel } from './navigation'

type SidebarNavItemProps = {
  item: SidebarNavItemModel
  active: boolean
  activeKey: PageKey
  expanded: boolean
  onSelect: (key: PageKey) => void
  onToggle: (key: PageKey | null) => void
}

export default function SidebarNavItem({ item, active, activeKey, expanded, onSelect, onToggle }: SidebarNavItemProps) {
  const selectParent = () => {
    onSelect(item.key)
    onToggle(item.children?.length ? (expanded ? null : item.key) : null)
  }

  return (
    <div className={item.children?.length ? 'nav-branch' : undefined}>
      <button
        aria-current={activeKey === item.key ? 'page' : undefined}
        aria-expanded={item.children?.length ? expanded : undefined}
        className={active ? 'nav-item nav-item--active' : 'nav-item'}
        onClick={selectParent}
      >
        <span>{item.mark}</span>
        {item.label}
        {item.children?.length ? <b className={expanded ? 'nav-item__toggle nav-item__toggle--open' : 'nav-item__toggle'}>›</b> : null}
      </button>
      {item.children?.length && expanded ? <div className="nav-children">
        {item.children.map(child => <button
          aria-current={activeKey === child.key ? 'page' : undefined}
          className={activeKey === child.key ? 'nav-child nav-child--active' : 'nav-child'}
          key={child.key}
          onClick={() => onSelect(child.key)}
        >{child.label}</button>)}
      </div> : null}
    </div>
  )
}
