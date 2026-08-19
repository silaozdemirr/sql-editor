import { lazy, Suspense, useCallback, useEffect, useState, useRef } from 'react';
import { FiChevronDown, FiChevronRight, FiCircle, FiDatabase, FiKey, FiLayers, FiLogOut, FiRefreshCw, FiTable, FiAlertCircle, FiCode, FiDownload, FiMap, FiX, FiEye } from 'react-icons/fi';
import { getSchema, getTableColumns, getTableDDL } from '../api/schemaApi';
const SqlEditor = lazy(() => import('./SqlEditor'));
const ErdViewer = lazy(() => import('./ErdViewer'));

const ColumnIcon = ({ column }) => (
  column.primaryKey ? <FiKey className="schema-key" aria-label="Birincil anahtar" /> : <FiCircle className="schema-column-dot" aria-hidden="true" />
);

function SchemaItem({ item, connectionToken, icon: Icon, onOpenDDL, onOpenTableData }) {
  const [isOpen, setIsOpen] = useState(false);
  const [columns, setColumns] = useState(null);
  const [error, setError] = useState('');
  const toggle = async () => {
    const nextOpen = !isOpen;
    setIsOpen(nextOpen);
    if (!nextOpen || columns) return;
    setError('');
    try { setColumns(await getTableColumns(connectionToken, item.name)); }
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

function SchemaGroup({ title, items, connectionToken, icon, onOpenDDL, onOpenTableData }) {
  const [isOpen, setIsOpen] = useState(true);
  const Icon = icon;
  return <li className="schema-group">
    <button className="tree-row group-row" type="button" onClick={() => setIsOpen((value) => !value)} aria-expanded={isOpen}>
      {isOpen ? <FiChevronDown /> : <FiChevronRight />}<Icon className="schema-group-icon" />
      <span className="tree-label">{title}</span><span className="tree-count">{items.length}</span>
    </button>
    {isOpen && <ul className="schema-list">{items.map((item) => <SchemaItem key={item.name} item={item} connectionToken={connectionToken} icon={title === 'Tablolar' ? FiTable : FiLayers} onOpenDDL={onOpenDDL} onOpenTableData={onOpenTableData} />)}</ul>}
  </li>;
}

function SavedScriptsGroup({ onOpenScript, currentDatabase }) {
  const [isOpen, setIsOpen] = useState(true);
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

export default function SchemaExplorer({ connectionInfo, onDisconnect, userRole }) {
  const [schema, setSchema] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showErd, setShowErd] = useState(false);
  const sqlEditorRef = useRef(null);

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

  const handleDumpDatabase = async () => {
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
      a.download = `${schema?.databaseName || 'database'}_dump.sql`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert("Veritabanı yedeği alınırken hata oluştu: " + err.message);
    }
  };

  const handleOpenScript = (name, query) => {
    if (sqlEditorRef.current) {
      sqlEditorRef.current.openTab(name, query);
    }
  };

  const loadSchema = useCallback(async () => {
    setIsLoading(true); setError('');
    try { setSchema(await getSchema(connectionInfo.connectionToken)); }
    catch (requestError) { setError(requestError.response?.data?.message || 'Şema bilgisi alınamadı.'); }
    finally { setIsLoading(false); }
  }, [connectionInfo.connectionToken]);
  const handleOpenTableData = (tableName) => {
    let sql = `SELECT * FROM ${tableName} LIMIT 100;`;
    if (connectionInfo.dbType === 'MSSQL') sql = `SELECT TOP 100 * FROM ${tableName};`;
    else if (connectionInfo.dbType === 'ORACLE') sql = `SELECT * FROM ${tableName} FETCH FIRST 100 ROWS ONLY;`;
    if (sqlEditorRef.current) {
      sqlEditorRef.current.openTab(tableName, sql, true);
    }
  };

  useEffect(() => { loadSchema(); }, [loadSchema]);
  return <main className="workspace">
    <aside className="schema-explorer" aria-label="Şema gezgini">
      <header className="explorer-header"><div><span className="panel-eyebrow">VERİTABANI GEZGİNİ</span><h1>Bağlantılar</h1></div>
        <button className="icon-button" type="button" onClick={loadSchema} disabled={isLoading} title="Şemayı yenile" aria-label="Şemayı yenile"><FiRefreshCw className={isLoading ? 'spin-icon' : ''} /></button>
      </header>
      <section className="connection-tree">
        <div className="tree-row database-row" style={{ display: 'flex', alignItems: 'center', width: '100%', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <FiChevronDown />
            <FiDatabase className="database-icon" />
            <span className="tree-label" style={{ marginRight: '6px' }}>{connectionInfo.connectionName || schema?.databaseName || connectionInfo.databaseName}</span>
            <span className="connected-indicator" title="Bağlı" />
          </div>
          <div style={{ display: 'flex', gap: '4px' }}>
            <button type="button" onClick={() => setShowErd(true)} title="ER Diyagramı Göster" style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }}>
              <FiMap size={14} />
            </button>
            <button type="button" onClick={handleDumpDatabase} title="Tüm Veritabanını Yedekle (.sql)" style={{ background: 'transparent', border: 'none', color: '#aaa', cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }}>
              <FiDownload size={14} />
            </button>
          </div>
        </div>
        {isLoading && <p className="schema-state">Şema yükleniyor </p>}
        {error && <div className="schema-state error-state"><FiAlertCircle /><span>{error}</span><button type="button" onClick={loadSchema}>Tekrar dene</button></div>}
        {schema && !isLoading && !error && <ul className="schema-list root-list">
          <SchemaGroup title="Tablolar" items={schema.tables || []} connectionToken={connectionInfo.connectionToken} icon={FiTable} onOpenDDL={handleOpenDDL} onOpenTableData={handleOpenTableData} />
          {schema?.views && schema.views.length > 0 && <SchemaGroup title="Görünümler" items={schema.views} connectionToken={connectionInfo.connectionToken} icon={FiEye} onOpenTableData={handleOpenTableData} />}
          <SavedScriptsGroup onOpenScript={(name, query) => sqlEditorRef.current?.openTab(name, query)} currentDatabase={schema?.databaseName || connectionInfo.databaseName || connectionInfo.database || 'varsayilan_db'} />
        </ul>}
      </section>
      <footer className="explorer-footer"><span><span className="connected-indicator" /> {connectionInfo.dbType === 'POSTGRESQL' ? 'PostgreSQL' : connectionInfo.dbType === 'ORACLE' ? 'Oracle' : connectionInfo.dbType === 'MSSQL' ? 'SQL Server' : connectionInfo.dbType === 'SQLITE' ? 'SQLite' : connectionInfo.dbType === 'MARIADB' ? 'MariaDB' : 'MySQL'} bağlı</span><button className="disconnect-link" type="button" onClick={onDisconnect}><FiLogOut /> Bağlantıyı kes</button></footer>
    </aside>
    <section className="workspace-main">
      <Suspense fallback={<section className="editor-loading">SQL editörü yükleniyor </section>}>
        <SqlEditor 
          ref={sqlEditorRef}
          connectionToken={connectionInfo.connectionToken} 
          currentDatabase={schema?.databaseName || connectionInfo.databaseName || connectionInfo.database || 'varsayilan_db'}
          userRole={userRole} 
          dbType={connectionInfo.dbType}
        />
      </Suspense>
    </section>

    {showErd && (
      <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ background: '#fff', width: '90vw', height: '90vh', borderRadius: '8px', display: 'flex', flexDirection: 'column', overflow: 'hidden', boxShadow: '0 10px 25px rgba(0,0,0,0.5)' }}>
          <header style={{ padding: '16px 20px', background: '#1a202c', color: '#fff', borderBottom: '1px solid #2d3748', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '600', display: 'flex', alignItems: 'center', gap: '10px', letterSpacing: '0.5px' }}><FiMap size={20} /> Entity Relationship Diagram (ERD)</h2>
            <button type="button" onClick={() => setShowErd(false)} style={{ background: 'rgba(255,255,255,0.1)', color: '#fff', border: 'none', cursor: 'pointer', padding: '6px', borderRadius: '4px', display: 'flex', alignItems: 'center', justifyContent: 'center', transition: 'background 0.2s' }} onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.2)'} onMouseLeave={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.1)'} title="Kapat">
              <FiX size={22} />
            </button>
          </header>
          <Suspense fallback={<div style={{ padding: '20px' }}>Yükleniyor...</div>}>
            <ErdViewer connectionToken={connectionInfo.connectionToken} />
          </Suspense>
        </div>
      </div>
    )}
  </main>;
}

