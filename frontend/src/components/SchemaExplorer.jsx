import { lazy, Suspense, useCallback, useEffect, useState, useRef } from 'react';
import { FiChevronDown, FiChevronRight, FiCircle, FiDatabase, FiKey, FiLayers, FiLogOut, FiRefreshCw, FiTable, FiAlertCircle, FiCode, FiDownload, FiMap, FiX, FiEye, FiPlus } from 'react-icons/fi';
import { getSchema, getTableColumns, getTableDDL } from '../api/schemaApi';
const SqlEditor = lazy(() => import('./SqlEditor'));
const ErdViewer = lazy(() => import('./ErdViewer'));

const ColumnIcon = ({ column }) => (
  column.primaryKey ? <FiKey className="schema-key" aria-label="Birincil anahtar" /> : <FiCircle className="schema-column-dot" aria-hidden="true" />
);

function SchemaItem({ item, connectionToken, currentDatabase, icon: Icon, onOpenDDL, onOpenTableData }) {
  const [isOpen, setIsOpen] = useState(false);
  const [columns, setColumns] = useState(null);
  const [error, setError] = useState('');
  const toggle = async () => {
    const nextOpen = !isOpen;
    setIsOpen(nextOpen);
    if (!nextOpen || columns) return;
    setError('');
    try { setColumns(await getTableColumns(connectionToken, item.name, currentDatabase)); }
    catch (requestError) { setError(requestError.response?.data?.message || 'Kolonlar yüklenemedi.'); }
  };
  return <li className="schema-item">
    <div style={{ display: 'flex', alignItems: 'center', width: '100%', justifyContent: 'space-between' }} className="schema-item-header">
      <button className="tree-row table-row" type="button" onClick={toggle} onDoubleClick={() => onOpenTableData && onOpenTableData(item.name)} aria-expanded={isOpen} style={{ flex: 1, textAlign: 'left', display: 'flex', alignItems: 'center' }}>
        {isOpen ? <FiChevronDown /> : <FiChevronRight />}<Icon className="schema-table-icon" />
        <span className="tree-label">{item.name}</span>{(item.type === 'TABLE' || item.type === 'BASE TABLE') && <span className="row-count">{item.rowCount ?? 0}</span>}
      </button>
      {onOpenDDL && (
        <button type="button" className="ddl-button" title="DDL Kodu (CREATE TABLE)" onClick={(e) => { e.stopPropagation(); onOpenDDL(item.name); }} style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', padding: '4px 8px', display: 'flex', alignItems: 'center' }}>
          <FiCode size={16} />
        </button>
      )}
    </div>
    {isOpen && <ul className="column-list">
      {!columns && !error && <li className="schema-message">Kolonlar yükleniyor…</li>}
      {error && <li className="schema-error"><FiAlertCircle /> {error}</li>}
      {columns?.map((column) => <li className="column-row" key={column.name} title={column.fullType}>
        <ColumnIcon column={column} /><span className="column-name">{column.name}</span>
        <span className="column-type">{column.fullType || column.dataType}</span>
        {column.unique && !column.primaryKey && <span className="column-badge">UQ</span>}
      </li>)}
    </ul>}
  </li>;
}

function SchemaGroup({ title, items, connectionToken, currentDatabase, icon, onOpenDDL, onOpenTableData, defaultOpen = false }) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  const Icon = icon;
  return <li className="schema-group">
    <button className="tree-row group-row" type="button" onClick={() => setIsOpen((value) => !value)} aria-expanded={isOpen}>
      {isOpen ? <FiChevronDown /> : <FiChevronRight />}<Icon className="schema-group-icon" />
      <span className="tree-label">{title}</span><span className="tree-count">{items.length}</span>
    </button>
    {isOpen && <ul className="schema-list">{items.map((item) => <SchemaItem key={item.name} item={item} connectionToken={connectionToken} currentDatabase={currentDatabase} icon={title === 'Tablolar' ? FiTable : FiLayers} onOpenDDL={onOpenDDL} onOpenTableData={onOpenTableData} />)}</ul>}
  </li>;
}

function SavedScriptsGroup({ onOpenScript, currentDatabase }) {
  const [isOpen, setIsOpen] = useState(false);
  const [scripts, setScripts] = useState([]);

  const loadScripts = () => {
    const all = JSON.parse(localStorage.getItem('savedScripts') || '[]');
    const filtered = all.filter(s => s.database === currentDatabase || (!s.database && currentDatabase === 'hastane_db'));
    setScripts(filtered);
  };

  useEffect(() => {
    loadScripts();
    window.addEventListener('savedScriptsUpdated', loadScripts);
    return () => window.removeEventListener('savedScriptsUpdated', loadScripts);
  }, []);

  const deleteScript = (e, id) => {
    e.stopPropagation();
    if (!window.confirm('Bu betiği silmek istediğinize emin misiniz?')) return;
    const newScripts = scripts.filter(s => s.id !== id);
    localStorage.setItem('savedScripts', JSON.stringify(newScripts));
    setScripts(newScripts);
  };

  return <li className="schema-group">
    <button className="tree-row group-row" type="button" onClick={() => setIsOpen((value) => !value)} aria-expanded={isOpen}>
      {isOpen ? <FiChevronDown /> : <FiChevronRight />}<FiCode className="schema-group-icon" />
      <span className="tree-label">Kayıtlı Sorgular</span><span className="tree-count">{scripts.length}</span>
    </button>
    {isOpen && <ul className="schema-list">
      {scripts.length === 0 && <li className="schema-message" style={{ paddingLeft: '32px' }}>Henüz kayıtlı sorgu yok.</li>}
      {scripts.map((script) => (
        <li className="schema-item" key={script.id}>
          <div style={{ display: 'flex', alignItems: 'center', width: '100%', justifyContent: 'space-between' }} className="schema-item-header">
            <button className="tree-row table-row" type="button" onClick={() => onOpenScript(script.name, script.query)} style={{ flex: 1, textAlign: 'left', display: 'flex', alignItems: 'center', paddingLeft: '32px' }}>
              <FiCode className="schema-table-icon" />
              <span className="tree-label">{script.name}</span>
            </button>
            <button type="button" className="ddl-button" title="Sil" onClick={(e) => deleteScript(e, script.id)} style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', padding: '4px 8px', display: 'flex', alignItems: 'center' }}>
              <FiX size={14} />
            </button>
          </div>
        </li>
      ))}
    </ul>}
  </li>;
}



function DatabaseNode({ dbName, connectionInfo, isActive, sqlEditorRef, onActiveTablesLoaded }) {
  const [schema, setSchema] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);
  const [showErd, setShowErd] = useState(false);

  const loadSchema = useCallback(async () => {
    setIsLoading(true); setError('');
    try { 
      const data = await getSchema(connectionInfo.connectionToken, dbName);
      setSchema(data); 
      if (isActive && onActiveTablesLoaded) {
        onActiveTablesLoaded(data?.tables?.map(t => t.name) || []);
      }
    }
    catch (requestError) { setError(requestError.response?.data?.message || 'Şema bilgisi alınamadı.'); }
    finally { setIsLoading(false); }
  }, [connectionInfo.connectionToken, dbName, isActive, onActiveTablesLoaded]);

  useEffect(() => { 
    if (schema && isActive && onActiveTablesLoaded && isExpanded) {
      onActiveTablesLoaded(schema?.tables?.map(t => t.name) || []);
    }
  }, [isActive, isExpanded, schema, onActiveTablesLoaded]);

  const toggle = (e) => {
    e.stopPropagation();
    if (!isExpanded && !schema) loadSchema();
    setIsExpanded(!isExpanded);
  };

  const handleOpenDDL = async (tableName) => {
    try {
      const ddl = await getTableDDL(connectionInfo.connectionToken, tableName);
      if (sqlEditorRef.current) {
        sqlEditorRef.current.openTab(`${tableName} DDL`, ddl);
      }
    } catch (err) {
      alert("DDL kodu alınamadı: " + (err.response?.data?.message || err.message));
    }
  };

  const handleDumpDatabase = async (e) => {
    e.stopPropagation();
    try {
      const jwtToken = sessionStorage.getItem('accessToken');
      const response = await fetch(`/api/schema/dump?database=${encodeURIComponent(dbName)}`, {
        method: 'GET',
        headers: {
          'X-Connection-Token': connectionInfo.connectionToken,
          ...(jwtToken ? { 'Authorization': `Bearer ${jwtToken}` } : {})
        },
      });

      if (!response.ok) {
        throw new Error(`Yedek alınamadı! (Hata kodu: ${response.status})`);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${dbName}_dump.sql`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert("Veritabanı yedeği alınırken hata oluştu: " + err.message);
    }
  };

  const handleOpenTableData = (tableName) => {
    // If the database isn't the default connection database, we might need to prefix the table, 
    // but the backend uses the connection context. We assume the backend switches catalog correctly.
    let sql = `SELECT * FROM \`${dbName}\`.\`${tableName}\` LIMIT 100;`;
    if (connectionInfo.dbType === 'MSSQL') sql = `SELECT TOP 100 * FROM [${dbName}].[dbo].[${tableName}];`;
    else if (connectionInfo.dbType === 'ORACLE') sql = `SELECT * FROM "${dbName}"."${tableName}" FETCH FIRST 100 ROWS ONLY;`;
    else if (connectionInfo.dbType === 'POSTGRESQL') sql = `SELECT * FROM "${tableName}" LIMIT 100;`; // Postgres uses current schema search path normally
    
    if (sqlEditorRef.current) {
      sqlEditorRef.current.openTab(tableName, sql, true);
    }
  };

  return (
    <>
      <div 
        className="tree-row database-row" 
        onClick={toggle}
        style={{ 
          display: 'flex', alignItems: 'center', width: '100%', justifyContent: 'space-between',
          cursor: 'pointer', paddingLeft: '16px', paddingRight: '8px'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center' }}>
          {isExpanded ? <FiChevronDown /> : <FiChevronRight />}
          <FiDatabase className="database-icon" style={{ margin: '0 6px', color: 'var(--text-secondary)' }} />
          <span className="tree-label" style={{ fontSize: '13px' }}>{dbName}</span>
        </div>
        <div style={{ display: 'flex', gap: '4px' }}>
          <button type="button" onClick={(e) => { e.stopPropagation(); setShowErd(true); }} title="ER Diyagramı Göster" style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }}>
            <FiMap size={12} />
          </button>
          <button type="button" onClick={handleDumpDatabase} title="Veritabanını Yedekle (.sql)" style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }}>
            <FiDownload size={12} />
          </button>
        </div>
      </div>
      
      {isExpanded && (
        <>
          {isLoading && <p className="schema-state" style={{ paddingLeft: '40px' }}>Yükleniyor...</p>}
          {error && <div className="schema-state error-state" style={{ paddingLeft: '40px' }}><FiAlertCircle /><span>{error}</span><button type="button" onClick={loadSchema}>Tekrar</button></div>}
          {schema && !isLoading && !error && <ul className="schema-list root-list" style={{ paddingLeft: '24px' }}>
            <SchemaGroup title="Tablolar" items={schema.tables || []} connectionToken={connectionInfo.connectionToken} currentDatabase={dbName} icon={FiTable} onOpenDDL={handleOpenDDL} onOpenTableData={handleOpenTableData} />
            {schema?.views && schema.views.length > 0 && <SchemaGroup title="Görünümler" items={schema.views} connectionToken={connectionInfo.connectionToken} currentDatabase={dbName} icon={FiEye} onOpenTableData={handleOpenTableData} />}
            <SavedScriptsGroup onOpenScript={(name, query) => sqlEditorRef.current?.openTab(name, query)} currentDatabase={dbName} />
          </ul>}
        </>
      )}

      {showErd && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'var(--bg-layer-1)', width: '90vw', height: '90vh', borderRadius: '8px', display: 'flex', flexDirection: 'column', overflow: 'hidden', boxShadow: '0 10px 25px rgba(0,0,0,0.5)' }}>
            <header style={{ padding: '16px 20px', background: 'var(--bg-layer-2)', color: 'var(--text-primary)', borderBottom: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '10px', letterSpacing: '0.5px' }}><FiMap size={20} /> Entity Relationship Diagram (ERD) - {dbName}</h2>
              <button type="button" onClick={() => setShowErd(false)} style={{ background: 'rgba(128,128,128,0.1)', color: 'var(--text-primary)', border: 'none', cursor: 'pointer', padding: '6px', borderRadius: '4px', display: 'flex', alignItems: 'center', justifyContent: 'center', transition: 'background 0.2s' }} title="Kapat">
                <FiX size={22} />
              </button>
            </header>
            <Suspense fallback={<div style={{ padding: '20px' }}>Yükleniyor...</div>}>
              <ErdViewer connectionToken={connectionInfo.connectionToken} database={dbName} />
            </Suspense>
          </div>
        </div>
      )}
    </>
  );
}

import { getDatabases } from '../api/schemaApi';

function ConnectionNode({ connectionInfo, isActive, onSelect, onDisconnect, sqlEditorRef, onActiveTablesLoaded }) {
  const [databases, setDatabases] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);
  const [showErd, setShowErd] = useState(false);

  const loadDatabases = useCallback(async () => {
    setIsLoading(true); setError('');
    try { 
      const data = await getDatabases(connectionInfo.connectionToken);
      const systemDbs = ['information_schema', 'performance_schema', 'mysql', 'sys'];
      setDatabases(data.filter(db => !systemDbs.includes(db.toLowerCase()))); 
    }
    catch (requestError) { setError(requestError.response?.data?.message || 'Veritabanları alınamadı.'); }
    finally { setIsLoading(false); }
  }, [connectionInfo.connectionToken]);

  useEffect(() => { 
    if (isExpanded && databases.length === 0 && !error) {
      loadDatabases();
    }
  }, [isExpanded, databases.length, loadDatabases, error]);

  const handleDumpDatabase = async (e) => {
    e.stopPropagation();
    try {
      const jwtToken = sessionStorage.getItem('accessToken');
      const response = await fetch(`/api/schema/dump`, {
        method: 'GET',
        headers: {
          'X-Connection-Token': connectionInfo.connectionToken,
          ...(jwtToken ? { 'Authorization': `Bearer ${jwtToken}` } : {})
        },
      });

      if (!response.ok) {
        throw new Error(`Yedek alınamadı! (Hata kodu: ${response.status})`);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${connectionInfo.connectionName || 'database'}_dump.sql`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert("Veritabanı yedeği alınırken hata oluştu: " + err.message);
    }
  };

  return (
    <>
      <div 
        className={`tree-row connection-server-row ${isActive ? 'active-db-row' : ''}`} 
        onClick={() => { onSelect(); setIsExpanded(!isExpanded); }}
        style={{ 
          display: 'flex', alignItems: 'center', width: '100%', justifyContent: 'space-between',
          cursor: 'pointer', background: isActive ? 'var(--bg-layer-2)' : 'transparent', borderLeft: isActive ? '3px solid var(--accent)' : '3px solid transparent',
          padding: '6px 8px'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center' }}>
          {isExpanded ? <FiChevronDown /> : <FiChevronRight />}
          <FiLayers className="database-icon" style={{ margin: '0 6px', color: 'var(--accent)' }} />
          <span className="tree-label" style={{ fontWeight: isActive ? '600' : 'normal', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            {connectionInfo.dbType || 'MYSQL'}
          </span>
          {isActive && <span className="connected-indicator" title="Aktif Bağlantı" style={{ marginLeft: '8px', flexShrink: 0 }} />}
        </div>
        <div style={{ display: 'flex', gap: '4px' }}>
          <button type="button" onClick={(e) => { e.stopPropagation(); setShowErd(true); }} title="ER Diyagramı Göster" style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }}>
            <FiMap size={14} />
          </button>
          <button type="button" onClick={handleDumpDatabase} title="Tüm Veritabanını Yedekle (.sql)" style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }}>
            <FiDownload size={14} />
          </button>
          <button type="button" onClick={(e) => { e.stopPropagation(); onDisconnect(); }} title="Bağlantıyı Kapat" style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }}>
            <FiX size={14} />
          </button>
        </div>
      </div>
      
      {isExpanded && (
        <div style={{ paddingBottom: '8px' }}>
          {isLoading && <p className="schema-state" style={{ paddingLeft: '24px' }}>Veritabanları yükleniyor...</p>}
          {error && <div className="schema-state error-state" style={{ paddingLeft: '24px' }}><FiAlertCircle /><span>{error}</span><button type="button" onClick={loadDatabases}>Tekrar</button></div>}
          {!isLoading && !error && databases.map(dbName => (
            <DatabaseNode 
              key={dbName} 
              dbName={dbName} 
              connectionInfo={connectionInfo}
              isActive={isActive}
              sqlEditorRef={sqlEditorRef}
              onActiveTablesLoaded={onActiveTablesLoaded}
            />
          ))}
          {!isLoading && !error && databases.length === 0 && (
             <p className="schema-state" style={{ paddingLeft: '24px' }}>Veritabanı bulunamadı.</p>
          )}
        </div>
      )}

      {showErd && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'var(--bg-layer-1)', width: '90vw', height: '90vh', borderRadius: '8px', display: 'flex', flexDirection: 'column', overflow: 'hidden', boxShadow: '0 10px 25px rgba(0,0,0,0.5)' }}>
            <header style={{ padding: '16px 20px', background: 'var(--bg-layer-2)', color: 'var(--text-primary)', borderBottom: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '10px', letterSpacing: '0.5px' }}><FiMap size={20} /> Entity Relationship Diagram (ERD) - {connectionInfo.connectionName}</h2>
              <button type="button" onClick={() => setShowErd(false)} style={{ background: 'rgba(128,128,128,0.1)', color: 'var(--text-primary)', border: 'none', cursor: 'pointer', padding: '6px', borderRadius: '4px', display: 'flex', alignItems: 'center', justifyContent: 'center', transition: 'background 0.2s' }} title="Kapat">
                <FiX size={22} />
              </button>
            </header>
            <Suspense fallback={<div style={{ padding: '20px' }}>Yükleniyor...</div>}>
              <ErdViewer connectionToken={connectionInfo.connectionToken} />
            </Suspense>
          </div>
        </div>
      )}
    </>
  );
}
export default function SchemaExplorer({ connections, activeToken, onSwitchConnection, onAddConnection, onDisconnectConnection, onDisconnectAll, userRole }) {
  const sqlEditorRefs = useRef({});
  const [activeTables, setActiveTables] = useState([]);
  const [splitToken, setSplitToken] = useState(null);
  
  const activeConnection = connections.find(c => c.connectionToken === activeToken) || connections[0];

  return <main className="workspace">
    <aside className="schema-explorer" aria-label="Şema gezgini">
      <header className="explorer-header">
        <div><span className="panel-eyebrow">VERİTABANI GEZGİNİ</span><h1>Bağlantılar</h1></div>
        <button 
          type="button" 
          onClick={onAddConnection} 
          title="Yeni Bağlantı Ekle" 
          aria-label="Yeni Bağlantı Ekle" 
          style={{ 
            padding: '6px 12px', 
            display: 'flex', 
            alignItems: 'center', 
            gap: '6px', 
            fontSize: '13px', 
            fontWeight: '600',
            background: 'linear-gradient(135deg, var(--accent) 0%, #4f46e5 100%)', 
            color: '#fff', 
            borderRadius: '6px', 
            border: 'none', 
            cursor: 'pointer',
            boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
            transition: 'transform 0.1s, boxShadow 0.2s'
          }}
          onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 4px 8px rgba(0,0,0,0.15)'; }}
          onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 2px 4px rgba(0,0,0,0.1)'; }}
        >
          <FiPlus size={16} /> Yeni Ekle
        </button>
      </header>
      <section className="connection-tree" style={{ padding: '0 8px' }}>
        {connections.map(conn => (
          <ConnectionNode 
            key={conn.connectionToken}
            connectionInfo={conn}
            isActive={conn.connectionToken === activeToken || conn.connectionToken === splitToken}
            onSelect={() => {
              if (splitToken && conn.connectionToken !== splitToken && conn.connectionToken !== activeToken) {
                onSwitchConnection(conn.connectionToken);
              } else {
                onSwitchConnection(conn.connectionToken);
              }
            }}
            onDisconnect={() => {
              if (splitToken === conn.connectionToken) setSplitToken(null);
              onDisconnectConnection(conn.connectionToken);
            }}
            sqlEditorRef={{
              get current() { return sqlEditorRefs.current[conn.connectionToken]; }
            }}
            userRole={userRole}
            onActiveTablesLoaded={conn.connectionToken === activeToken ? setActiveTables : undefined}
          />
        ))}
        {connections.length === 0 && (
          <div style={{ padding: '30px 20px', textAlign: 'center', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '10px', alignItems: 'center' }}>
            <FiDatabase size={32} style={{ opacity: 0.5 }} />
            <span style={{ fontSize: '13px' }}>Henüz bir veritabanı bağlı değil.</span>
            <span style={{ fontSize: '12px', opacity: 0.8 }}>Yukarıdaki <strong>+ Yeni Ekle</strong> butonuna tıklayarak sunucunuzu bağlayabilirsiniz.</span>
          </div>
        )}
      </section>
      <footer className="explorer-footer">
        <span>{connections.length} bağlantı açık</span>
        <button className="disconnect-link" type="button" onClick={onDisconnectAll}><FiLogOut /> Hepsini Kapat</button>
      </footer>
    </aside>
    <section className="workspace-main" style={{ display: 'flex', flexDirection: 'column' }}>
      <div className="workspace-tabs" style={{ display: 'flex', background: 'var(--bg-layer-1)', borderBottom: '1px solid var(--border-subtle)', overflowX: 'auto', padding: '6px 8px 0', gap: '4px' }}>
        {connections.map(conn => {
          const isActive = conn.connectionToken === activeToken || conn.connectionToken === splitToken;
          return (
            <div 
              key={conn.connectionToken}
              onClick={() => onSwitchConnection(conn.connectionToken)}
              style={{
                padding: '6px 12px',
                background: isActive ? 'var(--bg-card)' : 'transparent',
                border: '1px solid var(--border-subtle)',
                borderBottom: isActive ? '1px solid var(--bg-card)' : '1px solid var(--border-subtle)',
                marginBottom: '-1px',
                borderTopLeftRadius: '6px',
                borderTopRightRadius: '6px',
                cursor: 'pointer',
                color: isActive ? 'var(--text-primary)' : 'var(--text-secondary)',
                fontWeight: isActive ? '600' : 'normal',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '13px'
              }}
            >
              <FiDatabase size={14} style={{ color: isActive ? 'var(--accent)' : 'inherit' }} />
              {conn.connectionName || conn.databaseName || conn.database}
              
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  if (splitToken === conn.connectionToken) {
                    setSplitToken(null);
                  } else if (activeToken !== conn.connectionToken) {
                    setSplitToken(conn.connectionToken);
                  }
                }}
                title={splitToken === conn.connectionToken ? "Bölmeyi Kapat" : "Yan Yana Aç (Split View)"}
                style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: '2px', display: 'flex', borderRadius: '4px' }}
              >
                <FiLayers size={12} />
              </button>

              <button 
                type="button"
                onClick={(e) => { 
                  e.stopPropagation(); 
                  if (splitToken === conn.connectionToken) setSplitToken(null);
                  onDisconnectConnection(conn.connectionToken); 
                }}
                title="Kapat"
                style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: '2px', display: 'flex', borderRadius: '50%' }}
              >
                <FiX size={12} />
              </button>
            </div>
          );
        })}
      </div>
      <Suspense fallback={<section className="editor-loading">SQL editörü yükleniyor </section>}>
        <div style={{ display: 'flex', flex: 1, width: '100%', height: '100%', overflow: 'hidden' }}>
          {connections.length === 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', flex: 1, color: 'var(--text-muted)', gap: '16px', background: 'var(--bg-card)' }}>
              <FiDatabase size={64} style={{ opacity: 0.2 }} />
              <h2>SQL Editörüne Hoş Geldiniz</h2>
              <p>Sol menüden bir bağlantı ekleyerek veya seçerek başlayın.</p>
              <button className="btn-primary" onClick={onAddConnection} style={{ marginTop: '10px', flex: 'none', padding: '10px 24px', borderRadius: '8px' }}>
                + Yeni Veritabanı Ekle
              </button>
            </div>
          )}
          {connections.map(conn => {
            const isVisible = conn.connectionToken === activeToken || conn.connectionToken === splitToken;
            return (
              <div 
                key={conn.connectionToken} 
                style={{ 
                  display: isVisible ? 'flex' : 'none', 
                  flexDirection: 'column', 
                  flex: 1, 
                  height: '100%',
                  minWidth: 0,
                  borderRight: (isVisible && splitToken && conn.connectionToken === activeToken && activeToken !== splitToken) ? '2px solid var(--border-subtle)' : 'none'
                }}
              >
                <SqlEditor 
                  ref={el => sqlEditorRefs.current[conn.connectionToken] = el}
                  connectionToken={conn.connectionToken} 
                  currentDatabase={conn.databaseName || conn.database || 'varsayilan_db'}
                  userRole={userRole} 
                  dbType={conn.dbType}
                  tables={conn.connectionToken === activeToken ? activeTables : []}
                />
              </div>
            );
          })}
        </div>
      </Suspense>
    </section>
  </main>;
}

