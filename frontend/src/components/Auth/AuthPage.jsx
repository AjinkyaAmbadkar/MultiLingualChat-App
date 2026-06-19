import { useState, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useIsMobile } from '../../hooks/useIsMobile'
import { login, register, googleLogin } from '../../api/auth'
import { getMe } from '../../api/users'
import ContactModal from '../Contact/ContactModal'

const LANGUAGES = ['English','Spanish','French','German','Hindi','Marathi','Japanese','Portuguese','Mandarin']
const GOOGLE_CLIENT_ID = '341789138413-lc0evlvskf9mq747k8r5n2a4dle671if.apps.googleusercontent.com'

// Same message, auto-cycled through languages to demo the core feature live.
const DEMO = [
  { lang: 'Español',  text: '¡Hola! ¿Seguimos viéndonos hoy?' },
  { lang: 'हिन्दी',     text: 'नमस्ते! क्या हम आज भी मिल रहे हैं?' },
  { lang: '日本語',    text: 'やあ！今日はまだ会える？' },
  { lang: 'Français', text: "Salut ! On se voit toujours aujourd'hui ?" },
]

// Keyframes can't be expressed inline, so they live in one injected <style> block.
const KEYFRAMES = `
@keyframes plAurora  {0%{transform:translate(0,0) scale(1)}50%{transform:translate(30px,-20px) scale(1.15)}100%{transform:translate(0,0) scale(1)}}
@keyframes plAurora2 {0%{transform:translate(0,0) scale(1.1)}50%{transform:translate(-26px,24px) scale(.95)}100%{transform:translate(0,0) scale(1.1)}}
@keyframes plFloat   {0%{transform:translateY(0)}50%{transform:translateY(-8px)}100%{transform:translateY(0)}}
@keyframes plCaret   {0%,100%{opacity:1}50%{opacity:0}}
@keyframes plRise    {0%{opacity:0;transform:translateY(14px)}100%{opacity:1;transform:translateY(0)}}
@media (prefers-reduced-motion: reduce){
  .pl-aurora,.pl-chip,.pl-caret{animation:none !important}
}
`

const S = {
  page: (mobile) => ({
    minHeight: '100dvh', display: 'flex',
    flexDirection: mobile ? 'column' : 'row',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    background: '#0f172a',
  }),
  left: (mobile) => ({
    flex: mobile ? 'none' : 1, position: 'relative', overflow: 'hidden',
    display: 'flex', flexDirection: 'column', justifyContent: 'center',
    padding: mobile ? '28px 24px 24px' : '60px 64px',
    background: 'linear-gradient(160deg, #0f172a 0%, #1b2f5c 55%, #2742a8 100%)',
  }),
  blob: (cfg) => ({
    position: 'absolute', borderRadius: '50%', filter: 'blur(44px)',
    pointerEvents: 'none', ...cfg,
  }),
  inner: { position: 'relative', zIndex: 1 },
  right: (mobile) => ({
    width: mobile ? '100%' : '480px', flexShrink: 0, background: '#fff',
    display: 'flex', flexDirection: 'column', justifyContent: 'center',
    padding: mobile ? '28px 24px 36px' : '48px 48px',
    minHeight: mobile ? 'auto' : '100vh',
    borderRadius: mobile ? '20px 20px 0 0' : 0,
    flex: mobile ? 1 : 'none',
    boxSizing: 'border-box',
  }),
  logo: (mobile) => ({
    display: 'flex', alignItems: 'center', gap: '12px', marginBottom: mobile ? '14px' : '40px',
  }),
  logoText: { fontSize: '20px', fontWeight: 700, color: '#fff', letterSpacing: '-0.3px' },
  tagline: (mobile) => ({
    fontSize: mobile ? '24px' : '46px', fontWeight: 800, color: '#fff',
    lineHeight: 1.12, letterSpacing: mobile ? '-0.5px' : '-1px', marginBottom: mobile ? 0 : '18px',
  }),
  taglineAccent: {
    background: 'linear-gradient(90deg, #a78bfa, #38bdf8)',
    WebkitBackgroundClip: 'text', backgroundClip: 'text', color: 'transparent',
  },
  sub: { fontSize: '15px', color: '#94a3b8', lineHeight: 1.7, maxWidth: '400px', marginBottom: '26px' },

  demoCard: {
    background: 'rgba(255,255,255,0.07)', border: '1px solid rgba(255,255,255,0.12)',
    borderRadius: '16px', padding: '16px', maxWidth: '340px',
    backdropFilter: 'blur(4px)', animation: 'plRise .6s ease both',
  },
  bubbleOut: {
    background: '#1d4ed8', color: '#fff', fontSize: '14px', padding: '9px 13px',
    borderRadius: '14px 14px 4px 14px', maxWidth: '86%',
  },
  demoLabel: { display: 'flex', alignItems: 'center', gap: '7px', margin: '10px 0 7px' },
  demoDot: { width: '7px', height: '7px', borderRadius: '50%', background: '#38bdf8', flexShrink: 0 },
  bubbleIn: {
    background: '#fff', color: '#0f172a', fontSize: '14px', padding: '9px 13px',
    borderRadius: '14px 14px 14px 4px', maxWidth: '92%',
  },
  chips: { marginTop: '20px', display: 'flex', flexWrap: 'wrap', gap: '8px' },
  chip: (delay) => ({
    fontSize: '12px', color: '#cbd5e1', background: 'rgba(99,102,241,0.15)',
    border: '1px solid rgba(99,102,241,0.3)', borderRadius: '20px', padding: '5px 12px',
    animation: 'plFloat 4s ease-in-out infinite', animationDelay: `${delay}s`,
  }),

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
    // 16px so iOS Safari doesn't auto-zoom when the field gets focus
    borderRadius: '10px', fontSize: '16px', color: '#0f172a', background: '#f8fafc',
    outline: 'none', boxSizing: 'border-box', transition: 'all .2s',
    boxShadow: focused ? '0 0 0 3px rgba(59,130,246,0.12)' : 'none',
  }),
  field: { marginBottom: '16px' },
  btn: {
    width: '100%', padding: '13px', background: 'linear-gradient(135deg, #4f46e5, #1d4ed8)', color: '#fff',
    border: 'none', borderRadius: '12px', fontSize: '15px', fontWeight: 700,
    cursor: 'pointer', marginTop: '4px', letterSpacing: '0.1px',
    boxShadow: '0 4px 14px rgba(29,78,216,0.35)', transition: 'transform .15s ease, box-shadow .15s ease',
  },
  divider: { display: 'flex', alignItems: 'center', gap: '12px', margin: '20px 0' },
  divLine: { flex: 1, height: '1px', background: '#e2e8f0' },
  divText: { fontSize: '12px', color: '#94a3b8', whiteSpace: 'nowrap' },
  googleBtn: {
    width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
    gap: '10px', padding: '12px', border: '1.5px solid #e2e8f0', borderRadius: '12px',
    background: '#fff', fontSize: '14px', fontWeight: 600, color: '#374151', cursor: 'pointer',
    transition: 'background .2s',
  },
  error: {
    background: '#fef2f2', border: '1px solid #fecaca', color: '#dc2626',
    borderRadius: '10px', padding: '11px 14px', fontSize: '13px', marginBottom: '20px', lineHeight: 1.5,
  },
}

export default function AuthPage() {
  const { signIn } = useAuth()
  const isMobile = useIsMobile()
  const [tab, setTab]         = useState('login')
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)
  const [loginEmail, setLoginEmail]       = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [regName, setRegName]   = useState('')
  const [regEmail, setRegEmail] = useState('')
  const [regPass, setRegPass]   = useState('')
  const [regLang, setRegLang]   = useState('English')
  const [showContact, setShowContact] = useState(false)

  useEffect(() => {
    const s = document.createElement('script')
    s.src = 'https://accounts.google.com/gsi/client'; s.async = true
    document.body.appendChild(s)
    return () => document.body.removeChild(s)
  }, [])

  async function handleSignIn(res) {
    const me = await getMe(res.accessToken)
    await signIn(
      res.accessToken,
      { id: me.id, name: me.name, email: me.email, pictureUrl: me.pictureUrl, preferredLanguage: me.preferredLanguage },
      res.privateKey   // Base64 PKCS#8 — imported into memory, never stored
    )
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
    <div style={S.page(isMobile)}>
      <style>{KEYFRAMES}</style>

      {/* Branding — full panel on desktop, compact header strip on mobile */}
      <div style={S.left(isMobile)}>
        {!isMobile && (
          <>
            <div className="pl-aurora" style={S.blob({ width: '300px', height: '300px', top: '-80px', left: '-60px', background: 'radial-gradient(circle, #634bff, transparent 60%)', opacity: 0.5, animation: 'plAurora 9s ease-in-out infinite' })} />
            <div className="pl-aurora" style={S.blob({ width: '280px', height: '280px', bottom: '-70px', right: '-40px', background: 'radial-gradient(circle, #38bdf8, transparent 60%)', opacity: 0.38, animation: 'plAurora2 11s ease-in-out infinite' })} />
          </>
        )}

        <div style={S.inner}>
          <div style={S.logo(isMobile)}>
            <img src="/favicon.svg" alt="PolyLingual Chat" style={{ width: isMobile ? '42px' : '54px', height: isMobile ? '42px' : '54px' }} />
            <span style={S.logoText}>PolyLingual Chat</span>
          </div>

          {isMobile ? (
            <h1 style={S.tagline(true)}>
              Chat without <span style={S.taglineAccent}>language barriers</span>
            </h1>
          ) : (
            <>
              <h1 style={S.tagline(false)}>
                Chat without<br />
                <span style={S.taglineAccent}>language barriers.</span>
              </h1>

              <p style={S.sub}>
                Type in your language. Your friends read in theirs.
                Powered by OpenAI. Translation happens automatically, invisibly.
              </p>

              <TranslationDemo />

              <div style={S.chips}>
                {['English', 'हिन्दी', '日本語', 'Français', 'Español'].map((c, idx) => (
                  <span key={c} className="pl-chip" style={S.chip(idx * 0.6)}>{c}</span>
                ))}
              </div>
            </>
          )}
        </div>
      </div>

      {/* Form */}
      <div style={S.right(isMobile)}>
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
            <button type="submit" disabled={loading} style={{ ...S.btn, opacity: loading ? 0.7 : 1 }}
              onMouseEnter={e => { if (!loading) { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = '0 8px 22px rgba(99,75,255,0.45)' } }}
              onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(29,78,216,0.35)' }}
            >
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
            <button type="submit" disabled={loading} style={{ ...S.btn, opacity: loading ? 0.7 : 1 }}
              onMouseEnter={e => { if (!loading) { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = '0 8px 22px rgba(99,75,255,0.45)' } }}
              onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(29,78,216,0.35)' }}
            >
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

        <p style={{ marginTop: '22px', textAlign: 'center', fontSize: '13px', color: '#94a3b8' }}>
          Have feedback or want to connect?{' '}
          <button type="button" onClick={() => setShowContact(true)}
            style={{ background: 'none', border: 'none', padding: 0, color: '#1d4ed8', fontWeight: 600, fontSize: '13px', cursor: 'pointer' }}>
            Contact me
          </button>
        </p>
      </div>

      {showContact && <ContactModal onClose={() => setShowContact(false)} />}
    </div>
  )
}

// Live demo: one message cycling through languages with a typing caret.
function TranslationDemo() {
  const [i, setI] = useState(0)
  const [visible, setVisible] = useState(true)

  useEffect(() => {
    const id = setInterval(() => {
      setVisible(false)
      setTimeout(() => { setI(p => (p + 1) % DEMO.length); setVisible(true) }, 260)
    }, 2600)
    return () => clearInterval(id)
  }, [])

  return (
    <div style={S.demoCard}>
      <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
        <div style={S.bubbleOut}>Hey! Are we still meeting today?</div>
      </div>
      <div style={S.demoLabel}>
        <span style={S.demoDot} />
        <span style={{ fontSize: '12px', color: '#7dd3fc', fontWeight: 600, opacity: visible ? 1 : 0, transition: 'opacity .26s' }}>
          {DEMO[i].lang}
        </span>
        <span style={{ fontSize: '12px', color: '#64748b' }}>· Priya reads</span>
      </div>
      <div style={{ display: 'flex' }}>
        <div style={S.bubbleIn}>
          <span style={{ opacity: visible ? 1 : 0, transition: 'opacity .26s' }}>{DEMO[i].text}</span>
          <span className="pl-caret" style={{ color: '#1d4ed8', animation: 'plCaret 1s step-end infinite' }}>|</span>
        </div>
      </div>
    </div>
  )
}

function Field({ label, type = 'text', value, onChange, placeholder }) {
  const [focused, setFocused] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const isPassword = type === 'password'
  const inputType = isPassword ? (showPassword ? 'text' : 'password') : type
  return (
    <div style={S.field}>
      <label style={S.label}>{label}</label>
      <div style={{ position: 'relative' }}>
        <input type={inputType} value={value} placeholder={placeholder} required
          onChange={e => onChange(e.target.value)}
          onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}
          style={{ ...S.input(focused), paddingRight: isPassword ? '42px' : '14px' }}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setShowPassword(v => !v)}
            style={{
              position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)',
              background: 'none', border: 'none', cursor: 'pointer', padding: '2px',
              color: '#94a3b8', fontSize: '16px', lineHeight: 1,
            }}
            tabIndex={-1}
          >
            {showPassword ? '🙈' : '👁️'}
          </button>
        )}
      </div>
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
