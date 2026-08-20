import { useState, useEffect } from 'react';
import ConnectionPanel from './components/ConnectionPanel';
import SchemaExplorer from './components/SchemaExplorer';
import AdminPanel from './components/AdminPanel';
import './index.css';
import { disconnectDatabase, login, register, logout } from './api/connectionApi';

function AuthPanel({ onAuthenticated, toggleTheme, isLightMode }) {
  const [isRegister, setIsRegister] = useState(false);
  const [form, setForm] = useState({ email: '', password: '', displayName: '' });
  const [error, setError] = useState('');
  const submit = async (event) => { event.preventDefault(); setError(''); try { const result = await (isRegister ? register(form) : login(form)); sessionStorage.setItem('accessToken', result.accessToken); onAuthenticated(result); } catch (e) { setError(e.response?.data?.message || 'Giriş yapılamadı.'); } };
  return (
    <>
      <main className="connection-page auth-page" style={{ background: 'radial-gradient(ellipse 60% 40% at 50% 10%, rgba(59, 130, 246,0.18) 0%, transparent 60%), radial-gradient(ellipse 40% 40% at 20% 80%, rgba(59, 130, 246,0.12) 0%, transparent 50%), var(--bg-primary)' }}>
        <section className="auth-card" style={{ maxWidth: '340px', margin: '0 auto', boxShadow: '0 12px 40px rgba(59, 130, 246, 0.3)' }}>
          <div className="card-body">
            <h1 style={{ fontSize: '1.5rem', marginBottom: '4px' }}>{isRegister ? 'Hesap oluştur' : 'Giriş yap'}</h1>
            <p className="logo-subtitle" style={{ marginBottom: '1.5rem' }}>Bağlantıların yalnızca sana ait olur.</p>
            <form onSubmit={submit}>
              <div className="form-group"><label className="form-label">E-posta</label><input className="form-input" style={{ paddingLeft: '0.875rem' }} type="email" required value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></div>
              {isRegister && <div className="form-group"><label className="form-label">Görünen ad</label><input className="form-input" style={{ paddingLeft: '0.875rem' }} required minLength="2" value={form.displayName} onChange={e=>setForm({...form,displayName:e.target.value})}/></div>}
              <div className="form-group"><label className="form-label">Parola</label><input className="form-input" style={{ paddingLeft: '0.875rem' }} type="password" required minLength="8" value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/></div>
              {error && <p className="schema-error">{error}</p>}
              <button className="btn btn-primary" style={{ width: '100%', marginTop: '1.5rem', padding: '0.8rem', fontSize: '15px', borderRadius: '8px', flex: 'none' }} type="submit">{isRegister ? 'Kayıt ol' : 'Giriş yap'}</button>
            </form>
            <button className="disconnect-link" style={{ marginTop: '1rem', width: '100%', textAlign: 'center', color: 'var(--text-secondary)' }} type="button" onClick={()=>setIsRegister(!isRegister)}>{isRegister ? 'Zaten hesabın var mı? Giriş yap' : 'Hesabın yok mu? Kayıt ol'}</button>
          </div>
        </section>
      </main>
      <button onClick={toggleTheme} style={{ position: 'fixed', bottom: '20px', right: '20px', zIndex: 9999, background: 'var(--bg-card)', color: 'var(--text-primary)', border: '1px solid var(--border-subtle)', borderRadius: '50%', width: '40px', height: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', boxShadow: 'var(--shadow-md)' }} title={isLightMode ? "Karanlık Tema" : "Aydınlık Tema"}>
        {isLightMode ? <span style={{fontSize:'18px'}}>🌙</span> : <span style={{fontSize:'18px'}}>☀️</span>}
      </button>
    </>
  );
}

const getTokenRole = () => {
  const token = sessionStorage.getItem('accessToken');
  if (!token) return 'USER';
  try { return JSON.parse(atob(token.split('.')[1])).role || 'USER'; } catch { return 'USER'; }
};

function App() {
  const [connections, setConnections] = useState(() => {
    const savedConns = sessionStorage.getItem('connections');
    if (savedConns) return JSON.parse(savedConns);
    const old = sessionStorage.getItem('connectionInfo');
    return old ? [JSON.parse(old)] : [];
  });
  const [activeToken, setActiveToken] = useState(() => {
    const savedToken = sessionStorage.getItem('activeToken');
    if (savedToken) return savedToken;
    const old = sessionStorage.getItem('connectionInfo');
    return old ? JSON.parse(old).connectionToken : null;
  });
  
  const [showAddConnection, setShowAddConnection] = useState(false);
  const [authenticated, setAuthenticated] = useState(Boolean(sessionStorage.getItem('accessToken')));
  const [showAdminPanel, setShowAdminPanel] = useState(false);
  const [isLightMode, setIsLightMode] = useState(() => localStorage.getItem('theme') === 'light');

  useEffect(() => {
    if (isLightMode) {
      document.body.classList.add('light-mode');
      localStorage.setItem('theme', 'light');
    } else {
      document.body.classList.remove('light-mode');
      localStorage.setItem('theme', 'dark');
    }
    window.dispatchEvent(new Event('themeChanged'));
  }, [isLightMode]);

  const toggleTheme = () => setIsLightMode(!isLightMode);

  const handleConnected = (info) => {
    setConnections(prev => {
      const newConns = [...prev.filter(c => c.connectionToken !== info.connectionToken), info];
      sessionStorage.setItem('connections', JSON.stringify(newConns));
      return newConns;
    });
    setActiveToken(info.connectionToken);
    sessionStorage.setItem('activeToken', info.connectionToken);
    setShowAddConnection(false);
    window.history.pushState(null, '', '#workspace');
  };

  const handleDisconnect = async (tokenToDisconnect) => {
    if (!tokenToDisconnect) return;
    try { await disconnectDatabase(tokenToDisconnect); } catch (e) { console.error('Bağlantı kesme hatası', e); }
    
    setConnections(prev => {
      const newConns = prev.filter(c => c.connectionToken !== tokenToDisconnect);
      sessionStorage.setItem('connections', JSON.stringify(newConns));
      
      if (activeToken === tokenToDisconnect) {
        const nextActive = newConns.length > 0 ? newConns[0].connectionToken : null;
        setActiveToken(nextActive);
        sessionStorage.setItem('activeToken', nextActive || '');
        if (newConns.length === 0) {
          window.history.pushState(null, '', '#connect');
        }
      }
      return newConns;
    });
  };

  const handleLogout = async () => { 
    // Disconnect all
    for (const c of connections) {
      if (c.connectionToken) {
        try { await disconnectDatabase(c.connectionToken); } catch(e){}
      }
    }
    await logout(); 
    sessionStorage.removeItem('accessToken'); 
    sessionStorage.removeItem('connections'); 
    sessionStorage.removeItem('activeToken'); 
    sessionStorage.removeItem('connectionInfo'); // cleanup old
    setConnections([]); 
    setActiveToken(null);
    setAuthenticated(false);
    window.history.pushState(null, '', '#login');
  };

  useEffect(() => {
    const handlePopState = () => {
      const hash = window.location.hash;
      if (hash === '#workspace' && sessionStorage.getItem('connections')) {
        // Stay in workspace
      } else if (hash === '#connect') {
        if (connections.length > 0) {
          // Navigated back from workspace to connect
          connections.forEach(c => {
             if (c.connectionToken) {
               disconnectDatabase(c.connectionToken).catch(e => console.error(e));
             }
          });
          sessionStorage.removeItem('connections');
          sessionStorage.removeItem('activeToken');
          setConnections([]);
          setActiveToken(null);
        }
        if (!authenticated && sessionStorage.getItem('accessToken')) {
           setAuthenticated(true);
        }
      } else if (hash === '#login' || hash === '') {
        if (authenticated) {
          // Navigated back from connect to login
          connections.forEach(c => {
             if (c.connectionToken) {
               disconnectDatabase(c.connectionToken).catch(e => console.error(e));
             }
          });
          logout().catch(e => console.error(e));
          sessionStorage.removeItem('accessToken'); 
          sessionStorage.removeItem('connections'); 
          sessionStorage.removeItem('activeToken');
          setConnections([]); 
          setActiveToken(null);
          setAuthenticated(false);
        }
      }
    };
    
    const onSessionExpired = () => {
      handleLogout();
    };

    window.addEventListener('popstate', handlePopState);
    window.addEventListener('session-expired', onSessionExpired);
    return () => {
      window.removeEventListener('popstate', handlePopState);
      window.removeEventListener('session-expired', onSessionExpired);
    };
  }, [connections, authenticated]);

  const handleAuthenticated = (result) => {
    setAuthenticated(true);
    window.history.pushState(null, '', '#connect');
  };

  if (!authenticated) return <AuthPanel onAuthenticated={handleAuthenticated} toggleTheme={toggleTheme} isLightMode={isLightMode} />;

  const userRole = getTokenRole();

  return (
    <div className="app">
      {userRole === 'ADMIN' && (
        <div className="admin-bar">
          <button className="btn-secondary" onClick={() => setShowAdminPanel(true)}>Admin Paneli</button>
        </div>
      )}
      <SchemaExplorer 
        connections={connections} 
        activeToken={activeToken}
        onSwitchConnection={(token) => {
          setActiveToken(token);
          sessionStorage.setItem('activeToken', token);
        }}
        onAddConnection={() => setShowAddConnection(true)}
        onDisconnectConnection={handleDisconnect}
        onDisconnectAll={handleLogout}
        userRole={userRole} 
      />
      {showAdminPanel && <AdminPanel onClose={() => setShowAdminPanel(false)} />}
      
      {showAddConnection && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.7)', zIndex: 99999, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'auto' }}>
          <div style={{ position: 'relative', width: '100%', maxWidth: '900px', margin: 'auto' }}>
            <button onClick={() => setShowAddConnection(false)} style={{ position: 'absolute', top: '15px', right: '15px', background: 'var(--bg-layer-2)', border: '1px solid var(--border-subtle)', color: 'var(--text-primary)', borderRadius: '50%', width: '32px', height: '32px', cursor: 'pointer', zIndex: 10, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>X</button>
            <ConnectionPanel onConnected={handleConnected} onLogout={handleLogout} />
          </div>
        </div>
      )}

      <button onClick={toggleTheme} style={{ position: 'fixed', bottom: '20px', right: '20px', zIndex: 9999, background: 'var(--bg-card)', color: 'var(--text-primary)', border: '1px solid var(--border-subtle)', borderRadius: '50%', width: '40px', height: '40px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', boxShadow: 'var(--shadow-md)' }} title={isLightMode ? "Karanlık Tema" : "Aydınlık Tema"}>
        {isLightMode ? <span style={{fontSize:'18px'}}>🌙</span> : <span style={{fontSize:'18px'}}>☀️</span>}
      </button>
    </div>
  );
}

export default App;
