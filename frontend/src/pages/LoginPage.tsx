import { useState, type FormEvent } from 'react'
import { api } from '../api'
import { saveSession, type AuthSession } from '../auth'
import { loadPortalApp } from '../portalLoader'

type Props = { onLogin: (session: AuthSession) => void }

export default function LoginPage({ onLogin }: Props) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    void loadPortalApp().catch(() => undefined)
    try {
      const session = await api<AuthSession>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      })
      saveSession(session)
      onLogin(session)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể đăng nhập')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-brand-panel">
        <div className="login-brand"><div className="brand__mark">G</div><div><strong>GPG Union</strong><span>Internal Portal</span></div></div>
        <div className="login-brand-copy"><p className="eyebrow">Cổng công đoàn nội bộ</p><h1>Dữ liệu công đoàn.<br />Vận hành thống nhất.</h1><p>Quản lý đoàn viên, chính sách, vụ việc, hoạt động và báo cáo theo đúng phạm vi được phân quyền.</p></div>
        <div className="login-security-note"><span>●</span><div><strong>Kết nối nội bộ được bảo vệ</strong><small>Phiên làm việc tự động hết hạn khi hết thời gian cho phép.</small></div></div>
      </section>

      <section className="login-form-panel">
        <form className="login-card" onSubmit={event => void submit(event)}>
          <div className="login-card__mark">G</div>
          <p className="eyebrow">Cổng nội bộ</p>
          <h2>Đăng nhập hệ thống</h2>
          <p className="login-intro">Sử dụng tài khoản ADMIN hoặc USER đã được cấp.</p>
          <label className="field"><span>Tên đăng nhập</span><input autoFocus required autoComplete="username" value={username} onChange={event => setUsername(event.target.value)} placeholder="Nhập tên đăng nhập" /></label>
          <label className="field"><span>Mật khẩu</span><div className="password-field"><input required type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} placeholder="Nhập mật khẩu" /><button type="button" onClick={() => setShowPassword(value => !value)}>{showPassword ? 'Ẩn' : 'Hiện'}</button></div></label>
          {error && <div className="alert alert--danger login-error">{error}</div>}
          <button className="button button--primary login-submit" disabled={loading}>{loading ? 'Đang xác thực…' : 'Đăng nhập'}</button>
          <small className="login-help">Nếu quên mật khẩu, liên hệ quản trị hệ thống GPG.</small>
        </form>
      </section>
    </main>
  )
}
