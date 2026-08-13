import { useState } from 'react';
import ConnectionPanel from './components/ConnectionPanel';
import SchemaExplorer from './components/SchemaExplorer';
import './index.css';
import { disconnectDatabase, login, register, logout } from './api/connectionApi';

function AuthPanel({ onAuthenticated }) {
  const [isRegister, setIsRegister] = useState(false);
  const [form, setForm] = useState({ email: '', password: '', displayName: '' });
  const [error, setError] = useState('');
  const submit = async (event) => { event.preventDefault(); setError(''); try { const result = await (isRegister ? register(form) : login(form)); sessionStorage.setItem('accessToken', result.accessToken); onAuthenticated(result); } catch (e) { setError(e.response?.data?.message || 'Giriş yapılamadı.'); } };
  return <main className="connection-page"><section className="connection-card"><div className="card-body"><h1>{isRegister ? 'Hesap oluştur' : 'Giriş yap'}</h1><p className="logo-subtitle">Bağlantıların yalnızca sana ait olur.</p><form onSubmit={submit}><div className="form-group"><label className="form-label">E-posta</label><input className="form-input" type="email" required value={form.email} onChange={e=>setForm({...form,email:e.target.value})}/></div>{isRegister && <div className="form-group"><label className="form-label">Görünen ad</label><input className="form-input" required minLength="2" value={form.displayName} onChange={e=>setForm({...form,displayName:e.target.value})}/></div>}<div className="form-group"><label className="form-label">Parola</label><input className="form-input" type="password" required minLength="8" value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/></div>{error && <p className="schema-error">{error}</p>}<button className="connect-btn" type="submit">{isRegister ? 'Kayıt ol' : 'Giriş yap'}</button></form><button className="disconnect-link" type="button" onClick={()=>setIsRegister(!isRegister)}>{isRegister ? 'Zaten hesabın var mı? Giriş yap' : 'Hesabın yok mu? Kayıt ol'}</button></div></section></main>;
}

function App() {
  const [connectionInfo, setConnectionInfo] = useState(null);
  const [authenticated, setAuthenticated] = useState(Boolean(sessionStorage.getItem('accessToken')));

  const handleConnected = (info) => {
    setConnectionInfo(info);
  };

  const handleDisconnect = async () => {
    if (connectionInfo?.connectionToken) {
      try { await disconnectDatabase(connectionInfo.connectionToken); } catch { /* Oturum süresi dolmuş olabilir. */ }
    }
    setConnectionInfo(null);
  };

  const handleLogout = async () => { if (connectionInfo?.connectionToken) await handleDisconnect(); await logout(); sessionStorage.removeItem('accessToken'); setConnectionInfo(null); setAuthenticated(false); };

  if (!authenticated) return <AuthPanel onAuthenticated={() => setAuthenticated(true)} />;

  if (connectionInfo) {
    return (
      <SchemaExplorer connectionInfo={connectionInfo} onDisconnect={handleDisconnect} />
    );
  }

  return (
    <div className="app">
      <ConnectionPanel onConnected={handleConnected} onLogout={handleLogout} />
    </div>
  );
}

export default App;
