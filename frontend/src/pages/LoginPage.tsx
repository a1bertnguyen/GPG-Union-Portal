import { useState, type FormEvent } from 'react'
import { api } from '../api'
import { saveSession, type AuthSession } from '../auth'
import { loadPortalApp } from '../portalLoader'

type Props = {
  notice?: string
  onLogin: (session: AuthSession) => void
}

export default function LoginPage({ notice, onLogin }: Props) {
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
        <div className="login-brand-copy">
          <p className="eyebrow">Cổng công đoàn nội bộ</p>
          <h1>GPG Union</h1>
          <p className="login-brand-subtitle">Internal Portal</p>
          <div className="login-brand-values"><span>Dữ liệu công đoàn</span><span>Vận hành thống nhất</span></div>
        </div>
        <div className="login-security-note"><span>●</span><div><strong>Kết nối nội bộ được bảo vệ</strong><small>Phiên làm việc tự động hết hạn khi hết thời gian cho phép.</small></div></div>
      </section>

      <section className="login-form-panel">
        <form className="login-card" onSubmit={event => void submit(event)}>
          <div className="login-card__mark">G</div>
          <p className="eyebrow">Cổng nội bộ</p>
          <h2>Đăng nhập hệ thống</h2>
          <p className="login-intro">Sử dụng tài khoản ADMIN hoặc USER đã được cấp.</p>
          <label className="field"><span>Tên đăng nhập</span><input autoFocus required autoComplete="username" value={username} onChange={event => setUsername(event.target.value)} placeholder="Nhập tên đăng nhập" /></label>
          <label className="field"><span>Mật khẩu</span><div className="password-field"><input required type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} placeholder="Nhập mật khẩu" /><button className="login-password-toggle" type="button" aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} title={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'} onClick={() => setShowPassword(value => !value)}><svg aria-hidden="true" viewBox="0 0 24 24"><path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" /><circle cx="12" cy="12" r="2.75" /></svg></button></div></label>
          {notice && <div className="alert alert--danger login-error" role="alert">{notice}</div>}
          {error && <div className="alert alert--danger login-error" role="alert">{error}</div>}
          <button className="button button--primary login-submit" disabled={loading}>{loading ? 'Đang xác thực…' : 'Đăng nhập'}</button>
          <small className="login-help">Nếu quên mật khẩu, liên hệ quản trị hệ thống GPG.</small>
        </form>
      </section>
    </main>
  )
}
