import { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import { login, register, googleLogin } from '../../api/auth'
import { getMe } from '../../api/users'

const LANGUAGES = [
  'English','Spanish','French','German','Hindi',
  'Marathi','Japanese','Portuguese','Mandarin',
]

const GOOGLE_CLIENT_ID = '341789138413-lc0evlvskf9mq747k8r5n2a4dle671if.apps.googleusercontent.com'

export default function AuthPage() {
  const { signIn } = useAuth()
  const [tab, setTab]         = useState('login')
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)

  const [loginEmail, setLoginEmail]       = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [regName, setRegName]             = useState('')
  const [regEmail, setRegEmail]           = useState('')
  const [regPass, setRegPass]             = useState('')
  const [regLang, setRegLang]             = useState('English')

  useEffect(() => {
    const script = document.createElement('script')
    script.src = 'https://accounts.google.com/gsi/client'
    script.async = true
    document.body.appendChild(script)
    return () => document.body.removeChild(script)
  }, [])

  async function handleSignIn(authResponse) {
    const me = await getMe(authResponse.accessToken)
    signIn(authResponse.accessToken, {
      id: me.id, name: me.name, email: me.email,
      pictureUrl: me.pictureUrl, preferredLanguage: me.preferredLanguage
    })
  }

  async function handleLogin(e) {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      await handleSignIn(await login(loginEmail, loginPassword))
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  async function handleRegister(e) {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      await handleSignIn(await register(regName, regEmail, regPass, regLang))
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }

  function triggerGoogle() {
    if (typeof google === 'undefined') { setError('Google SDK not loaded yet, try again in a moment.'); return }
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
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #1e3a5f 0%, #2d6a9f 50%, #1a2f4e 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    }}>
      <div style={{
        background: '#ffffff',
        borderRadius: '20px',
        boxShadow: '0 25px 60px rgba(0,0,0,0.35)',
        width: '100%',
        maxWidth: '440px',
        overflow: 'visible',
      }}>

        {/* Header */}
        <div style={{
          background: 'linear-gradient(135deg, #2563eb, #3b82f6)',
          borderRadius: '20px 20px 0 0',
          padding: '28px 32px 24px',
          color: '#fff',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
            <span style={{ fontSize: '24px' }}>💬</span>
            <h1 style={{ margin: 0, fontSize: '22px', fontWeight: 700, letterSpacing: '-0.3px' }}>
              MultiLingual Chat
            </h1>
          </div>
          <p style={{ margin: 0, fontSize: '13px', opacity: 0.85 }}>
            Talk to anyone, in any language
          </p>
        </div>

        {/* Tab switcher */}
        <div style={{ display: 'flex', borderBottom: '2px solid #f1f5f9' }}>
          {['login','register'].map(t => (
            <button key={t} onClick={() => { setTab(t); setError('') }} style={{
              flex: 1, padding: '14px', border: 'none', background: 'transparent',
              fontSize: '14px', fontWeight: 600, cursor: 'pointer', transition: 'all .2s',
              color: tab === t ? '#2563eb' : '#94a3b8',
              borderBottom: tab === t ? '2px solid #2563eb' : '2px solid transparent',
              marginBottom: '-2px',
            }}>
              {t === 'login' ? 'Sign In' : 'Create Account'}
            </button>
          ))}
        </div>

        {/* Form body */}
        <div style={{ padding: '28px 32px 32px' }}>

          {error && (
            <div style={{
              background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626',
              borderRadius: '10px', padding: '12px 16px', fontSize: '13px',
              marginBottom: '20px', lineHeight: 1.5,
            }}>
              {error}
            </div>
          )}

          {tab === 'login' ? (
            <form onSubmit={handleLogin}>
              <Field label="Email" type="email" value={loginEmail} onChange={setLoginEmail} placeholder="you@example.com" />
              <Field label="Password" type="password" value={loginPassword} onChange={setLoginPassword} placeholder="Enter your password" />
              <SubmitBtn loading={loading}>Sign In</SubmitBtn>
            </form>
          ) : (
            <form onSubmit={handleRegister}>
              <Field label="Full Name" value={regName} onChange={setRegName} placeholder="Your name" />
              <Field label="Email" type="email" value={regEmail} onChange={setRegEmail} placeholder="you@example.com" />
              <Field label="Password" type="password" value={regPass} onChange={setRegPass} placeholder="At least 8 characters" />
              <div style={{ marginBottom: '16px' }}>
                <label style={labelStyle}>Preferred Language</label>
                <select value={regLang} onChange={e => setRegLang(e.target.value)} style={inputStyle}>
                  {LANGUAGES.map(l => <option key={l}>{l}</option>)}
                </select>
              </div>
              <SubmitBtn loading={loading}>Create Account</SubmitBtn>
            </form>
          )}

          {/* Divider */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', margin: '20px 0', color: '#cbd5e1', fontSize: '12px' }}>
            <div style={{ flex: 1, height: '1px', background: '#e2e8f0' }} />
            <span style={{ color: '#94a3b8' }}>or continue with</span>
            <div style={{ flex: 1, height: '1px', background: '#e2e8f0' }} />
          </div>

          {/* Google button */}
          <button onClick={triggerGoogle} disabled={loading} style={{
            width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
            gap: '10px', padding: '12px', border: '1.5px solid #e2e8f0', borderRadius: '12px',
            background: '#fff', fontSize: '14px', fontWeight: 500, color: '#374151',
            cursor: 'pointer', transition: 'all .2s',
          }}
          onMouseEnter={e => e.currentTarget.style.background = '#f8fafc'}
          onMouseLeave={e => e.currentTarget.style.background = '#fff'}
          >
            <GoogleIcon />
            Continue with Google
          </button>

        </div>
      </div>
    </div>
  )
}

const labelStyle = {
  display: 'block', fontSize: '13px', fontWeight: 600,
  color: '#374151', marginBottom: '6px',
}

const inputStyle = {
  width: '100%', padding: '11px 14px', border: '1.5px solid #e2e8f0',
  borderRadius: '10px', fontSize: '14px', color: '#1e293b',
  background: '#f8fafc', outline: 'none', boxSizing: 'border-box',
  transition: 'border-color .2s',
}

function Field({ label, type = 'text', value, onChange, placeholder }) {
  const [focused, setFocused] = useState(false)
  return (
    <div style={{ marginBottom: '16px' }}>
      <label style={labelStyle}>{label}</label>
      <input
        type={type} value={value} placeholder={placeholder} required
        onChange={e => onChange(e.target.value)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        style={{ ...inputStyle, borderColor: focused ? '#2563eb' : '#e2e8f0',
          boxShadow: focused ? '0 0 0 3px rgba(37,99,235,0.1)' : 'none' }}
      />
    </div>
  )
}

function SubmitBtn({ children, loading }) {
  return (
    <button type="submit" disabled={loading} style={{
      width: '100%', padding: '13px', background: loading ? '#93c5fd' : '#2563eb',
      color: '#fff', border: 'none', borderRadius: '12px', fontSize: '15px',
      fontWeight: 600, cursor: loading ? 'not-allowed' : 'pointer',
      marginTop: '4px', marginBottom: '4px', transition: 'background .2s',
      letterSpacing: '0.1px',
    }}>
      {loading ? 'Please wait…' : children}
    </button>
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
