import { lazy, Suspense, useCallback, useEffect, useState } from 'react';
import { FiChevronDown, FiChevronRight, FiCircle, FiDatabase, FiKey, FiLayers, FiLogOut, FiRefreshCw, FiTable, FiAlertCircle } from 'react-icons/fi';
import { getSchema, getTableColumns } from '../api/schemaApi';
const SqlEditor = lazy(() => import('./SqlEditor'));

const ColumnIcon = ({ column }) => (
  column.primaryKey ? <FiKey className="schema-key" aria-label="Birincil anahtar" /> : <FiCircle className="schema-column-dot" aria-hidden="true" />
);

function SchemaItem({ item, connectionToken, icon: Icon }) {
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
    <button className="tree-row table-row" type="button" onClick={toggle} aria-expanded={isOpen}>
      {isOpen ? <FiChevronDown /> : <FiChevronRight />}<Icon className="schema-table-icon" />
      <span className="tree-label">{item.name}</span>{item.type === 'TABLE' && <span className="row-count">{item.rowCount ?? 0}</span>}
    </button>
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

function SchemaGroup({ title, items, connectionToken, icon }) {
  const [isOpen, setIsOpen] = useState(true);
  const Icon = icon;
  return <li className="schema-group">
    <button className="tree-row group-row" type="button" onClick={() => setIsOpen((value) => !value)} aria-expanded={isOpen}>
      {isOpen ? <FiChevronDown /> : <FiChevronRight />}<Icon className="schema-group-icon" />
      <span className="tree-label">{title}</span><span className="tree-count">{items.length}</span>
    </button>
    {isOpen && <ul className="schema-list">{items.map((item) => <SchemaItem key={item.name} item={item} connectionToken={connectionToken} icon={title === 'Tablolar' ? FiTable : FiLayers} />)}</ul>}
  </li>;
}

export default function SchemaExplorer({ connectionInfo, onDisconnect, userRole }) {
  const [schema, setSchema] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const loadSchema = useCallback(async () => {
    setIsLoading(true); setError('');
    try { setSchema(await getSchema(connectionInfo.connectionToken)); }
    catch (requestError) { setError(requestError.response?.data?.message || 'Şema bilgisi alınamadı.'); }
    finally { setIsLoading(false); }
  }, [connectionInfo.connectionToken]);
  useEffect(() => { loadSchema(); }, [loadSchema]);
  return <main className="workspace">
    <aside className="schema-explorer" aria-label="Şema gezgini">
      <header className="explorer-header"><div><span className="panel-eyebrow">DATABASE EXPLORER</span><h1>Bağlantılar</h1></div>
        <button className="icon-button" type="button" onClick={loadSchema} disabled={isLoading} title="Şemayı yenile" aria-label="Şemayı yenile"><FiRefreshCw className={isLoading ? 'spin-icon' : ''} /></button>
      </header>
      <section className="connection-tree"><div className="tree-row database-row"><FiChevronDown /><FiDatabase className="database-icon" /><span className="tree-label">{connectionInfo.connectionName || schema?.databaseName || connectionInfo.databaseName}</span><span className="connected-indicator" title="Bağlı" /></div>
        {isLoading && <p className="schema-state">Şema yükleniyor…</p>}
        {error && <div className="schema-state error-state"><FiAlertCircle /><span>{error}</span><button type="button" onClick={loadSchema}>Tekrar dene</button></div>}
        {schema && !isLoading && !error && <ul className="schema-list root-list"><SchemaGroup title="Tablolar" items={schema.tables || []} connectionToken={connectionInfo.connectionToken} icon={FiTable} /><SchemaGroup title="Görünümler" items={schema.views || []} connectionToken={connectionInfo.connectionToken} icon={FiLayers} /></ul>}
      </section>
      <footer className="explorer-footer"><span><span className="connected-indicator" /> MySQL bağlı</span><button className="disconnect-link" type="button" onClick={onDisconnect}><FiLogOut /> Bağlantıyı kes</button></footer>
    </aside>
    <section className="workspace-main">
      <Suspense fallback={<section className="editor-loading">SQL editörü yükleniyor…</section>}>
        <SqlEditor 
          connectionToken={connectionInfo.connectionToken} 
          userRole={userRole} 
        />
      </Suspense>
    </section>
  </main>;
}
