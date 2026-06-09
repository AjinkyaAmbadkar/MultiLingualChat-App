import { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import { login, register, googleLogin } from '../../api/auth'
import { getMe } from '../../api/users'

const LANGUAGES = ['English','Spanish','French','German','Hindi','Marathi','Japanese','Portuguese','Mandarin']
const GOOGLE_CLIENT_ID = '341789138413-lc0evlvskf9mq747k8r5n2a4dle671if.apps.googleusercontent.com'

const S = {
  page: {
    minHeight: '100vh', display: 'flex',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    background: '#0f172a',
  },
  left: {
    flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center',
    padding: '60px 64px',
    background: 'linear-gradient(160deg, #0f172a 0%, #1e3a5f 60%, #1e40af 100%)',
  },
  right: {
    width: '480px', flexShrink: 0, background: '#fff',
    display: 'flex', flexDirection: 'column', justifyContent: 'center',
    padding: '48px 48px',
    minHeight: '100vh',
  },
  logo: {
    display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '48px',
  },
  logoIcon: {
    width: '48px', height: '48px', borderRadius: '14px',
    background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontSize: '24px',
  },
  logoText: { fontSize: '20px', fontWeight: 700, color: '#fff', letterSpacing: '-0.3px' },
  tagline: {
    fontSize: '48px', fontWeight: 800, color: '#fff',
    lineHeight: 1.1, letterSpacing: '-1px', marginBottom: '24px',
  },
  taglineAccent: { color: '#60a5fa' },
  sub: { fontSize: '16px', color: '#94a3b8', lineHeight: 1.7, maxWidth: '420px' },
  features: { marginTop: '48px', display: 'flex', flexDirection: 'column', gap: '16px' },
  feature: { display: 'flex', alignItems: 'center', gap: '14px' },
  featureIcon: {
    width: '36px', height: '36px', borderRadius: '10px',
    background: 'rgba(59,130,246,0.15)', border: '1px solid rgba(59,130,246,0.3)',
    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '16px', flexShrink: 0,
  },
  featureText: { fontSize: '14px', color: '#cbd5e1' },

  formTitle: { fontSize: '26px', fontWeight: 800, color: '#0f172a', marginBottom: '6px', letterSpacing: '-0.5px' },
  formSub: { fontSize: '14px', color: '#64748b', marginBottom: '28px' },
  tabs: { display: 'flex', background: '#f1f5f9', borderRadius: '10px', padding: '3px', marginBottom: '24px' },
  tab: (active) => ({
    flex: 1, padding: '9px', border: 'none', borderRadius: '8px', cursor: 'pointer',
    fontSize: '14px', fontWeight: 600, transition: 'all .2s',
    background: active ? '#fff' : 'transparent',
    color: active ? '#1e40af' : '#64748b',
    boxShadow: active ? '0 1px 4px rgba(0,0,0,0.1)' : 'none',
  }),
  label: { display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' },
  input: (focused) => ({
    width: '100%', padding: '11px 14px', border: `1.5px solid ${focused ? '#3b82f6' : '#e2e8f0'}`,
    borderRadius: '10px', fontSize: '14px', color: '#0f172a', background: '#f8fafc',
    outline: 'none', boxSizing: 'border-box', transition: 'all .2s',
    boxShadow: focused ? '0 0 0 3px rgba(59,130,246,0.12)' : 'none',
  }),
  field: { marginBottom: '16px' },
  btn: {
    width: '100%', padding: '13px', background: '#1d4ed8', color: '#fff',
    border: 'none', borderRadius: '12px', fontSize: '15px', fontWeight: 700,
    cursor: 'pointer', marginTop: '4px', letterSpacing: '0.1px',
    boxShadow: '0 4px 14px rgba(29,78,216,0.35)',
  },
  divider: { display: 'flex', alignItems: 'center', gap: '12px', margin: '20px 0' },
  divLine: { flex: 1, height: '1px', background: '#e2e8f0' },
  divText: { fontSize: '12px', color: '#94a3b8', whiteSpace: 'nowrap' },
  googleBtn: {
    width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
    gap: '10px', padding: '12px', border: '1.5px solid #e2e8f0', borderRadius: '12px',
    background: '#fff', fontSize: '14px', fontWeight: 600, color: '#374151', cursor: 'pointer',
  },
  error: {
    background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626',
    borderRadius: '10px', padding: '11px 14px', fontSize: '13px', marginBottom: '20px', lineHeight: 1.5,
  },
}

export default function AuthPage() {
  const { signIn } = useAuth()
  const [tab, setTab]         = useState('login')
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)
  const [loginEmail, setLoginEmail]       = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [regName, setRegName]   = useState('')
  const [regEmail, setRegEmail] = useState('')
  const [regPass, setRegPass]   = useState('')
  const [regLang, setRegLang]   = useState('English')

  useEffect(() => {
    const s = document.createElement('script')
    s.src = 'https://accounts.google.com/gsi/client'; s.async = true
    document.body.appendChild(s)
    return () => document.body.removeChild(s)
  }, [])

  async function handleSignIn(res) {
    const me = await getMe(res.accessToken)
    signIn(res.accessToken, { id: me.id, name: me.name, email: me.email, pictureUrl: me.pictureUrl, preferredLanguage: me.preferredLanguage })
  }

  async function handleLogin(e) {
    e.preventDefault(); setError(''); setLoading(true)
    try { await handleSignIn(await login(loginEmail, loginPassword)) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  async function handleRegister(e) {
    e.preventDefault(); setError(''); setLoading(true)
    try { await handleSignIn(await register(regName, regEmail, regPass, regLang)) }
    catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  function triggerGoogle() {
    if (typeof google === 'undefined') { setError('Google SDK not loaded, try again in a moment.'); return }
    google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: async (response) => {
        setError(''); setLoading(true)
        try { await handleSignIn(await googleLogin(response.credential)) }
        catch (err) { setError(err.message) }
        finally { setLoading(false) }
      },
    })
    google.accounts.id.prompt()
  }

  return (
    <div style={S.page}>
      {/* Left — branding */}
      <div style={S.left}>
        <div style={S.logo}>
          <div style={S.logoIcon}>💬</div>
          <span style={S.logoText}>MultiLingual Chat</span>
        </div>

        <h1 style={S.tagline}>
          Chat without<br />
          <span style={S.taglineAccent}>Hassle or</span><br />
          Language Barrier
        </h1>

        <p style={S.sub}>
          Type in your language. Your friends read in theirs.
          Powered by OpenAI — translation happens automatically, invisibly.
        </p>

        <div style={S.features}>
          {[
            { icon: '🌍', text: 'Supports English, Spanish, Hindi, Japanese, French & more' },
            { icon: '⚡', text: 'Real-time delivery over WebSocket — zero delay' },
            { icon: '🔒', text: 'Secured with JWT + Google OAuth2 — your data is yours' },
            { icon: '💰', text: 'Translation called only when languages differ — cost efficient' },
          ].map(f => (
            <div key={f.icon} style={S.feature}>
              <div style={S.featureIcon}>{f.icon}</div>
              <span style={S.featureText}>{f.text}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Right — form */}
      <div style={S.right}>
        <div style={S.formTitle}>Welcome back</div>
        <div style={S.formSub}>Sign in to your account or create a new one</div>

        {/* Tabs */}
        <div style={S.tabs}>
          {['login','register'].map(t => (
            <button key={t} onClick={() => { setTab(t); setError('') }} style={S.tab(tab === t)}>
              {t === 'login' ? 'Sign In' : 'Create Account'}
            </button>
          ))}
        </div>

        {error && <div style={S.error}>{error}</div>}

        {tab === 'login' ? (
          <form onSubmit={handleLogin}>
            <Field label="Email" type="email" value={loginEmail} onChange={setLoginEmail} placeholder="you@example.com" />
            <Field label="Password" type="password" value={loginPassword} onChange={setLoginPassword} placeholder="Enter your password" />
            <button type="submit" disabled={loading} style={{ ...S.btn, opacity: loading ? 0.7 : 1 }}>
              {loading ? 'Signing in…' : 'Sign In →'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleRegister}>
            <Field label="Full Name" value={regName} onChange={setRegName} placeholder="Your name" />
            <Field label="Email" type="email" value={regEmail} onChange={setRegEmail} placeholder="you@example.com" />
            <Field label="Password" type="password" value={regPass} onChange={setRegPass} placeholder="At least 8 characters" />
            <div style={S.field}>
              <label style={S.label}>Preferred Language</label>
              <select value={regLang} onChange={e => setRegLang(e.target.value)} style={S.input(false)}>
                {LANGUAGES.map(l => <option key={l}>{l}</option>)}
              </select>
            </div>
            <button type="submit" disabled={loading} style={{ ...S.btn, opacity: loading ? 0.7 : 1 }}>
              {loading ? 'Creating account…' : 'Create Account →'}
            </button>
          </form>
        )}

        <div style={S.divider}>
          <div style={S.divLine} />
          <span style={S.divText}>or continue with</span>
          <div style={S.divLine} />
        </div>

        <button onClick={triggerGoogle} disabled={loading} style={S.googleBtn}
          onMouseEnter={e => e.currentTarget.style.background = '#f8fafc'}
          onMouseLeave={e => e.currentTarget.style.background = '#fff'}
        >
          <GoogleIcon /> Continue with Google
        </button>
      </div>
    </div>
  )
}

function Field({ label, type = 'text', value, onChange, placeholder }) {
  const [focused, setFocused] = useState(false)
  return (
    <div style={S.field}>
      <label style={S.label}>{label}</label>
      <input type={type} value={value} placeholder={placeholder} required
        onChange={e => onChange(e.target.value)}
        onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
        style={S.input(focused)}
      />
    </div>
  )
}

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 48 48">
      <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
      <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
      <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
      <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.18 1.48-4.97 2.31-8.16 2.31-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
    </svg>
  )
}
