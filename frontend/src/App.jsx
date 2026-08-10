import { useState } from 'react';
import { FiCheckCircle, FiDatabase } from 'react-icons/fi';
import ConnectionPanel from './components/ConnectionPanel';
import './index.css';

function App() {
  const [connectionInfo, setConnectionInfo] = useState(null);

  const handleConnected = (info) => {
    setConnectionInfo(info);
  };

  const handleDisconnect = () => {
    setConnectionInfo(null);
  };

  // Eğer bağlantı kurulduysa placeholder editör ekranını göster
  if (connectionInfo) {
    return (
      <div className="editor-placeholder">
        <FiCheckCircle size={56} color="var(--accent-green)" />
        <h2>Bağlantı Kuruldu! 🎉</h2>
        <p>
          <strong style={{ color: 'var(--text-primary)' }}>{connectionInfo.databaseName}</strong>
          {' '}veritabanına başarıyla bağlandın
        </p>
        <p style={{ fontSize: '0.85rem' }}>
          {connectionInfo.serverVersion}
        </p>
        <div className="session-chip">
          <FiDatabase size={12} />
          Session: {connectionInfo.sessionId?.substring(0, 16)}...
        </div>
        <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '0.5rem' }}>
          🚧 SQL Editör — Aşama 2: Schema Explorer yakında!
        </p>
        <button
          className="disconnect-btn"
          onClick={handleDisconnect}
          id="btn-disconnect"
        >
          ⬅ Bağlantıyı Kes
        </button>
      </div>
    );
  }

  return (
    <div className="app">
      <ConnectionPanel onConnected={handleConnected} />
    </div>
  );
}

export default App;
