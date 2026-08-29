import { useEffect, useState } from 'react'
import { loadEnumLabels } from './api'
import { apiAllCached } from './apiCache'
import type { AuthSession } from './auth'
import Sidebar from './components/sidebar/Sidebar'
import { getPageLabel, type PageKey } from './components/sidebar/navigation'
import PortalPage from './portal/PortalPage'
import type { UnionUnit } from './types'

type Props = {
  session: AuthSession
  onLogout: () => void
}

export default function PortalApp({ session, onLogout }: Props) {
  const [active, setActive] = useState<PageKey>(() => session.user.role === 'ADMIN' ? 'dashboard' : 'home')
  const [units, setUnits] = useState<UnionUnit[]>([])
  const [menuOpen, setMenuOpen] = useState(false)
  const isAdmin = session.user.role === 'ADMIN'

  useEffect(() => {
    // Every CĐCS dropdown reads this list. The short-lived shared cache prevents StrictMode from
    // issuing the same initial request twice while still keeping it scoped to the signed-in user.
    apiAllCached<UnionUnit>('/units').then(setUnits).catch(() => setUnits([]))
    void loadEnumLabels()
  }, [session])

  const selectPage = (key: PageKey) => {
    setActive(key)
    setMenuOpen(false)
  }

  return (
    <div className="app-shell">
      <header className="portal-topbar">
        <button className="portal-menu-button" aria-label="Mở menu" onClick={() => setMenuOpen(value => !value)}>☰</button>
        <div className="portal-brand"><div className="portal-brand__mark">G</div><div><strong>GPG UNION PORTAL</strong><span>Genuine Partner Trade Union</span></div></div>
        <div className="portal-context"><i /> <strong>{getPageLabel(active) ?? 'Tổng quan'}</strong></div>
        <div className="portal-user"><div><strong>{session.user.fullName}</strong><span>{isAdmin ? 'ADMIN · Toàn hệ thống' : `USER · ${session.user.unionUnitCode}`}</span></div><button onClick={onLogout}>Đăng xuất</button></div>
      </header>
      <Sidebar
        active={active}
        isAdmin={isAdmin}
        isOpen={menuOpen}
        onSelect={selectPage}
        unitName={session.user.unionUnitName}
      />
      <main className="main-content">
        <PortalPage
          active={active}
          session={session}
          units={units}
          onNavigate={selectPage}
        />
      </main>
      {menuOpen && <button className="menu-scrim" aria-label="Đóng menu" onClick={() => setMenuOpen(false)} />}
    </div>
  )
}
