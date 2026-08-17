import { useEffect, useState } from 'react';
import {
  FiDatabase, FiServer, FiUser, FiLock, FiEye, FiEyeOff,
  FiCheckCircle, FiAlertCircle, FiLoader, FiWifi, FiZap,
  FiInfo, FiLogOut, FiSave, FiTrash2
} from 'react-icons/fi';
import { SiMysql } from 'react-icons/si';
import { testConnection, connectToDatabase, connectSavedConnection, deleteSavedConnection, getSavedConnections, saveConnection } from '../api/connectionApi';

// Veritabanı tip konfigürasyonları
const DB_TYPES = [
  {
    key: 'MYSQL',
    label: 'MySQL',
    icon: '🐬',
    defaultPort: 3306,
    dotClass: 'mysql',
    placeholder: {
      host: 'localhost',
    },
  },
  {
    key: 'POSTGRESQL',
    label: 'PostgreSQL',
    icon: '🐘',
    defaultPort: 5432,
    dotClass: 'pg',
    badge: 'yakında',
    disabled: true,
    placeholder: {
      host: 'localhost',
      database: 'postgres',
      username: 'postgres',
    },
  },
];

const ConnectionPanel = ({ onConnected, onLogout }) => {
  const [selectedDb, setSelectedDb] = useState('MYSQL');
  const [showPassword, setShowPassword] = useState(false);
  const [testStatus, setTestStatus] = useState(null); // null | 'testing' | 'success' | 'error'
  const [connectStatus, setConnectStatus] = useState(null);
  const [testResult, setTestResult] = useState(null);
  const [isConnecting, setIsConnecting] = useState(false);
  const [isTesting, setIsTesting] = useState(false);
  const [savedConnections, setSavedConnections] = useState([]);
  const [savedError, setSavedError] = useState('');

  const currentDb = DB_TYPES.find(d => d.key === selectedDb);

  const [form, setForm] = useState({
    host: 'localhost',
    port: currentDb?.defaultPort || 3306,
    database: '',
    username: '',
    password: '',
    dbType: 'MYSQL',
    connectionName: '',
  });

  const loadSavedConnections = async () => {
    try { setSavedConnections(await getSavedConnections()); }
    catch { setSavedError('Kayıtlı bağlantılar yüklenemedi.'); }
  };

  useEffect(() => { loadSavedConnections(); }, []);

  const handleDbChange = (db) => {
    if (db.disabled) return;
    const dbConf = DB_TYPES.find(d => d.key === db.key);
    setSelectedDb(db.key);
    setTestStatus(null);
    setTestResult(null);
    setForm(prev => ({
      ...prev,
      dbType: db.key,
      port: dbConf.defaultPort,
      host: 'localhost',
      database: '',
      username: '',
    }));
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    // Input değişince test sonucunu sıfırla
    setTestStatus(null);
    setTestResult(null);
  };

  // Test Connection
  const handleTest = async () => {
    setIsTesting(true);
    setTestStatus('testing');
    setTestResult(null);

    try {
      const result = await testConnection({
        ...form,
        port: parseInt(form.port, 10),
      });

      setTestResult(result);
      setTestStatus(result.success ? 'success' : 'error');
    } catch (err) {
      setTestResult({
        success: false,
        message: 'Backend\'e ulaşılamıyor. Spring Boot çalışıyor mu?',
        errorDetail: err.message,
      });
      setTestStatus('error');
    } finally {
      setIsTesting(false);
    }
  };

  // Connect
  const handleConnect = async () => {
    setIsConnecting(true);
    setConnectStatus('connecting');

    try {
      const result = await connectToDatabase({
        ...form,
        port: parseInt(form.port, 10),
      });

      if (result.success) {
        setConnectStatus('success');
        result.connectionName = form.connectionName || form.database;
        // Kısa bir gecikme ile geçiş animasyonu
        setTimeout(() => {
          onConnected(result);
        }, 600);
      } else {
        setConnectStatus('error');
        setTestResult(result);
        setTestStatus('error');
      }
    } catch (err) {
      setConnectStatus('error');
      setTestResult({
        success: false,
        message: 'Backend\'e ulaşılamıyor.',
        errorDetail: err.message,
      });
      setTestStatus('error');
    } finally {
      setIsConnecting(false);
    }
  };

  const handleSave = async () => {
    if (!form.connectionName.trim()) {
      setSavedError('Kaydetmek için bir bağlantı adı girin.');
      return;
    }
    try {
      setSavedError('');
      await saveConnection({ ...form, port: parseInt(form.port, 10) });
      await loadSavedConnections();
    } catch (err) {
      setSavedError(err.response?.data?.message || 'Bağlantı kaydedilemedi. Aynı isim zaten kullanılıyor olabilir.');
    }
  };

  const handleSavedConnect = async (id) => {
    setIsConnecting(true);
    try {
      const result = await connectSavedConnection(id);
      if (result.success) {
        const conn = savedConnections.find(c => c.id === id);
        result.connectionName = conn ? conn.connectionName : result.databaseName;
        onConnected(result);
      }
      else setSavedError(result.message || 'Bağlantı kurulamadı.');
    } catch (err) {
      setSavedError(err.response?.data?.message || 'Kayıtlı bağlantı kurulamadı.');
    } finally { setIsConnecting(false); }
  };

  const handleDeleteSaved = async (id) => {
    try { await deleteSavedConnection(id); await loadSavedConnections(); }
    catch (err) { setSavedError(err.response?.data?.message || 'Kayıtlı bağlantı silinemedi.'); }
  };

  const isLoading = isTesting || isConnecting;

  return (
    <div className="connection-page">
      {/* Header */}
      <div className="connection-header">
        <div className="logo-area">
          <div className="logo-icon">⚡</div>
          <span className="logo-title">SQLEditör</span>
        </div>
        <p className="logo-subtitle">Kendi veritabanı yönetim aracın</p>
        <button className="logout-button" type="button" onClick={onLogout}>
          <FiLogOut /> Çıkış yap
        </button>
      </div>

      {/* Connection Card */}
      <div className="connection-card">
        {/* DB Type Tabs */}
        <div className="card-tabs">
          {DB_TYPES.map(db => (
            <button
              key={db.key}
              id={`tab-${db.key.toLowerCase()}`}
              className={`tab-btn ${selectedDb === db.key ? 'active' : ''} ${db.disabled ? 'disabled' : ''}`}
              onClick={() => handleDbChange(db)}
              disabled={db.disabled}
              title={db.disabled ? 'Yakında eklenecek' : db.label}
            >
              <span className={`db-dot ${db.dotClass}`}></span>
              {db.icon} {db.label}
              {db.badge && <span className="tab-badge">{db.badge}</span>}
            </button>
          ))}
        </div>

        {/* Form */}
        <div className="card-body">
          {/* Test/Connect sonuç mesajı */}
          {testStatus && testResult && (
            <div className={`status-badge ${testStatus}`}>
              <span className="status-icon">
                {testStatus === 'testing'  && <div className="btn-spinner" style={{borderTopColor: 'var(--accent-blue)'}} />}
                {testStatus === 'success'  && <FiCheckCircle />}
                {testStatus === 'error'    && <FiAlertCircle />}
              </span>
              <div className="status-text">
                <strong>{testResult.message}</strong>
                {testResult.serverVersion && (
                  <small>Sunucu: {testResult.serverVersion}</small>
                )}
                {testResult.errorDetail && testStatus === 'error' && (
                  <small style={{opacity: 0.7}}>{testResult.errorDetail}</small>
                )}
              </div>
              {testResult.responseTimeMs && (
                <span className="status-time">{testResult.responseTimeMs}ms</span>
              )}
            </div>
          )}

          {testStatus === 'testing' && !testResult && (
            <div className="status-badge testing">
              <span className="status-icon"><div className="btn-spinner" /></span>
              <div className="status-text">
                <strong>Bağlantı test ediliyor...</strong>
                <small>{form.host}:{form.port}</small>
              </div>
            </div>
          )}

          {/* Host + Port */}
          <div className="form-row two-col">
            <div className="form-group">
              <label className="form-label" htmlFor="host">
                Host <span className="required">*</span>
              </label>
              <div className="input-wrapper">
                <FiServer className="input-icon" />
                <input
                  id="host"
                  name="host"
                  type="text"
                  className="form-input"
                  value={form.host}
                  onChange={handleChange}
                  placeholder="localhost"
                  disabled={isLoading}
                  autoComplete="off"
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="port">Port</label>
              <input
                id="port"
                name="port"
                type="number"
                className="form-input port"
                value={form.port}
                onChange={handleChange}
                min="1"
                max="65535"
                disabled={isLoading}
              />
            </div>
          </div>

          {/* Database */}
          <div className="form-row single">
            <div className="form-group">
              <label className="form-label" htmlFor="database">
                Veritabanı <span className="required">*</span>
              </label>
              <div className="input-wrapper">
                <FiDatabase className="input-icon" />
                <input
                  id="database"
                  name="database"
                  type="text"
                  className="form-input"
                  value={form.database}
                  onChange={handleChange}
                  placeholder="Örn. hastane_db"
                  disabled={isLoading}
                  autoComplete="off"
                />
              </div>
            </div>
          </div>

          <div className="form-divider">
            <span>Kimlik Bilgileri</span>
          </div>

          <div className="form-row single">
            <div className="form-group">
              <label className="form-label" htmlFor="connectionName">Bağlantı Adı</label>
              <input id="connectionName" name="connectionName" type="text" className="form-input"
                value={form.connectionName} onChange={handleChange} placeholder="Örn. Üretim MySQL" disabled={isLoading} maxLength="100" />
            </div>
          </div>

          {/* Username */}
          <div className="form-row single">
            <div className="form-group">
              <label className="form-label" htmlFor="username">
                Kullanıcı Adı <span className="required">*</span>
              </label>
              <div className="input-wrapper">
                <FiUser className="input-icon" />
                <input
                  id="username"
                  name="username"
                  type="text"
                  className="form-input"
                  value={form.username}
                  onChange={handleChange}
                  placeholder="Kullanıcı adı girin"
                  disabled={isLoading}
                  autoComplete="off"
                />
              </div>
            </div>
          </div>

          {/* Password */}
          <div className="form-row single">
            <div className="form-group">
              <label className="form-label" htmlFor="password">Şifre</label>
              <div className="input-wrapper">
                <FiLock className="input-icon" />
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  className="form-input"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  disabled={isLoading}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  className="password-toggle"
                  onClick={() => setShowPassword(v => !v)}
                  tabIndex={-1}
                  title={showPassword ? 'Şifreyi gizle' : 'Şifreyi göster'}
                >
                  {showPassword ? <FiEyeOff /> : <FiEye />}
                </button>
              </div>
            </div>
          </div>

          {/* Başarılı bağlantı sonrası sunucu bilgisi */}
          {testStatus === 'success' && testResult && (
            <div className="server-info">
              <div className="info-item">
                <span className="info-label">Sunucu</span>
                <span className="info-value">{testResult.serverVersion || '-'}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Veritabanı</span>
                <span className="info-value">{testResult.databaseName || form.database}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Host</span>
                <span className="info-value">{form.host}:{form.port}</span>
              </div>
              <div className="info-item">
                <span className="info-label">Yanıt Süresi</span>
                <span className="info-value">{testResult.responseTimeMs}ms</span>
              </div>
            </div>
          )}

          {/* Buttons */}
          <div className="card-actions">
            <button
              id="btn-test-connection"
              type="button"
              className="btn btn-secondary"
              onClick={handleTest}
              disabled={isLoading}
              title="Bağlantıyı test et"
            >
              {isTesting ? (
                <><div className="btn-spinner" /> Test ediliyor...</>
              ) : (
                <><FiWifi /> Test Et</>
              )}
            </button>

            <button type="button" className="btn btn-secondary" onClick={handleSave} disabled={isLoading} title="Bağlantıyı güvenle kaydet">
              <FiSave /> Kaydet
            </button>

            <button
              id="btn-connect"
              type="button"
              className="btn btn-primary"
              onClick={handleConnect}
              disabled={isLoading}
            >
              {isConnecting ? (
                <><div className="btn-spinner" /> Bağlanıyor...</>
              ) : (
                <><FiZap /> Bağlan</>
              )}
            </button>
          </div>

          <section className="saved-connections" aria-label="Kayıtlı bağlantılar">
            <div className="saved-connections-heading"><span>Kayıtlı Bağlantılar</span><button type="button" className="refresh-saved" onClick={loadSavedConnections}>Yenile</button></div>
            {savedError && <p className="saved-error">{savedError}</p>}
            {!savedError && savedConnections.length === 0 && <p className="saved-empty">Henüz kayıtlı bağlantı yok.</p>}
            {savedConnections.map((connection) => <div className="saved-connection" key={connection.id}>
              <button type="button" className="saved-connect" onClick={() => handleSavedConnect(connection.id)} disabled={isLoading}>
                <strong>{connection.connectionName}</strong><small>{connection.host}:{connection.port} · {connection.databaseName}</small>
              </button>
              <button type="button" className="saved-delete" onClick={() => handleDeleteSaved(connection.id)} aria-label={`${connection.connectionName} bağlantısını sil`} title="Bağlantıyı sil"><FiTrash2 /></button>
            </div>)}
          </section>
        </div>

        {/* Card Footer */}
        <div className="card-footer">
          <span className="footer-tip">
            <FiInfo size={12} />
            Docker ile MySQL container'ı çalıştırdığından emin ol
          </span>
          <span className="footer-version">v0.1.0-alpha</span>
        </div>
      </div>
    </div>
  );
};

export default ConnectionPanel;
