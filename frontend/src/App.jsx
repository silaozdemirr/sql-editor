import { useState } from 'react';
import ConnectionPanel from './components/ConnectionPanel';
import SchemaExplorer from './components/SchemaExplorer';
import './index.css';

function App() {
  const [connectionInfo, setConnectionInfo] = useState(null);

  const handleConnected = (info) => {
    setConnectionInfo(info);
  };

  const handleDisconnect = () => {
    setConnectionInfo(null);
  };

  if (connectionInfo) {
    return (
      <SchemaExplorer connectionInfo={connectionInfo} onDisconnect={handleDisconnect} />
    );
  }

  return (
    <div className="app">
      <ConnectionPanel onConnected={handleConnected} />
    </div>
  );
}

export default App;
