import { useState, useEffect } from 'react';
import ConnectionPanel from './components/ConnectionPanel';
import SchemaExplorer from './components/SchemaExplorer';
import AdminPanel from './components/AdminPanel';
import './index.css';
import { disconnectDatabase, login, register, logout } from './api/connectionApi';

function AuthPanel({ onAuthenticated }) {
  const [isRegister, setIsRegister] = useState(false);
  const [form, setForm] = useState({ email: '', password: '', displayName: '' });
  const [error, setError] = useState('');
  const submit = async (event) => { event.preventDefault(); setError(''); try { const result = await (isRegister ? register(form) : login(form)); sessionStorage.setItem('accessToken', result.accessToken); onAuthenticated(result); } catch (e) { setError(e.response?.data?.message || 'Giriş yapılamadı.'); } };
  return <main className="connection-page"><section className="connection-card"><div className="card-body"><h1>{isRegister ? 'Hesap oluştur' : 'Giriş yap'}</h1><p className="logo-subtitle">Bağlantıların yalnızca sana ait olur.</p><form onSubmit={submit}><div className="form-group"><label className="form-label">E-posta</label><input className="form-input" style={{ paddingLeft: '0.875rem' }} type="email" required value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></div>{isRegister && <div className="form-group"><label className="form-label">Görünen ad</label><input className="form-input" style={{ paddingLeft: '0.875rem' }} required minLength="2" value={form.displayName} onChange={e=>setForm({...form,displayName:e.target.value})}/></div>}<div className="form-group"><label className="form-label">Parola</label><input className="form-input" style={{ paddingLeft: '0.875rem' }} type="password" required minLength="8" value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/></div>{error && <p className="schema-error">{error}</p>}<button className="btn btn-primary" style={{ width: '100%', marginTop: '1rem', padding: '0.75rem' }} type="submit">{isRegister ? 'Kayıt ol' : 'Giriş yap'}</button></form><button className="disconnect-link" style={{ marginTop: '1rem' }} type="button" onClick={()=>setIsRegister(!isRegister)}>{isRegister ? 'Zaten hesabın var mı? Giriş yap' : 'Hesabın yok mu? Kayıt ol'}</button></div></section></main>;
}

const getTokenRole = () => {
  const token = sessionStorage.getItem('accessToken');
  if (!token) return 'USER';
  try { return JSON.parse(atob(token.split('.')[1])).role || 'USER'; } catch { return 'USER'; }
};

function App() {
  const [connectionInfo, setConnectionInfo] = useState(() => {
    const saved = sessionStorage.getItem('connectionInfo');
    return saved ? JSON.parse(saved) : null;
  });
  const [authenticated, setAuthenticated] = useState(Boolean(sessionStorage.getItem('accessToken')));
  const [showAdminPanel, setShowAdminPanel] = useState(false);

  const handleConnected = (info) => {
    sessionStorage.setItem('connectionInfo', JSON.stringify(info));
    setConnectionInfo(info);
  };

  const handleDisconnect = async () => {
    if (connectionInfo?.connectionToken) {
      try { await disconnectDatabase(connectionInfo.connectionToken); } catch (e) { console.error('Bağlantı kesme hatası', e); }
    }
    sessionStorage.removeItem('connectionInfo');
    setConnectionInfo(null);
  };

  const handleLogout = async () => { if (connectionInfo?.connectionToken) await handleDisconnect(); await logout(); sessionStorage.removeItem('accessToken'); sessionStorage.removeItem('connectionInfo'); setConnectionInfo(null); setAuthenticated(false); };

  useEffect(() => {
    const onSessionExpired = () => {
      handleLogout();
    };
    window.addEventListener('session-expired', onSessionExpired);
    return () => window.removeEventListener('session-expired', onSessionExpired);
  }, [connectionInfo]);

  if (!authenticated) return <AuthPanel onAuthenticated={() => setAuthenticated(true)} />;

  const userRole = getTokenRole();

  if (connectionInfo) {
    return (
      <div className="app">
        {userRole === 'ADMIN' && (
          <div className="admin-bar">
            <button className="btn-secondary" onClick={() => setShowAdminPanel(true)}>Admin Paneli</button>
          </div>
        )}
        <SchemaExplorer connectionInfo={connectionInfo} onDisconnect={handleDisconnect} userRole={userRole} />
        {showAdminPanel && <AdminPanel onClose={() => setShowAdminPanel(false)} />}
      </div>
    );
  }

  return (
    <div className="app">
      {userRole === 'ADMIN' && (
        <div className="admin-bar">
          <button className="btn-secondary" onClick={() => setShowAdminPanel(true)}>Admin Paneli</button>
        </div>
      )}
      <ConnectionPanel onConnected={handleConnected} onLogout={handleLogout} />
      {showAdminPanel && <AdminPanel onClose={() => setShowAdminPanel(false)} />}
    </div>
  );
}

export default App;
