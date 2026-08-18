import React, { useCallback, useState, useEffect, useRef, forwardRef, useImperativeHandle } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { sql, MySQL } from '@codemirror/lang-sql';
import { oneDark } from '@codemirror/theme-one-dark';
import { FiCode, FiPlay, FiX, FiCheck, FiRotateCcw, FiAlignLeft } from 'react-icons/fi';
import { format } from 'sql-formatter';
import { executeQuery, explainQuery, manageTransaction } from '../api/queryApi';
import QueryResults from './QueryResults';

const INITIAL_SQL = `-- Sorgunuzu buraya yazın\n`;

const SqlEditor = forwardRef(({ connectionToken, currentDatabase, userRole }, ref) => {
  const [tabs, setTabs] = useState([{
    id: 1, title: 'SQL Query 1', query: INITIAL_SQL, notice: 'Sorguyu çalıştırmak için Ctrl + Enter kullanın.', isRunning: false, queryResult: null, queryError: '', explainResult: null, explainError: ''
  }]);

  useImperativeHandle(ref, () => ({
    openTab: (title, query) => {
      const id = Date.now();
      setTabs(prev => [...prev, {
        id, title, query, notice: '', isRunning: false, queryResult: null, queryError: '', explainResult: null, explainError: ''
      }]);
      setActiveTabId(id);
    }
  }));
  const [activeTabId, setActiveTabId] = useState(1);
  const [autoCommit, setAutoCommit] = useState(true);
  const [editorHeight, setEditorHeight] = useState(300);
  const [isLightMode, setIsLightMode] = useState(() => document.body.classList.contains('light-mode'));
  const isDragging = useRef(false);

  useEffect(() => {
    const handleThemeChange = () => setIsLightMode(document.body.classList.contains('light-mode'));
    window.addEventListener('themeChanged', handleThemeChange);
    return () => window.removeEventListener('themeChanged', handleThemeChange);
  }, []);

  const startDrag = (e) => {
    e.preventDefault();
    isDragging.current = true;
    document.addEventListener('mousemove', onDrag);
    document.addEventListener('mouseup', stopDrag);
  };

  const onDrag = (e) => {
    if (!isDragging.current) return;
    const newHeight = e.clientY - 40; 
    if (newHeight > 100 && newHeight < window.innerHeight - 100) {
      setEditorHeight(newHeight);
    }
  };

  const stopDrag = () => {
    isDragging.current = false;
    document.removeEventListener('mousemove', onDrag);
    document.removeEventListener('mouseup', stopDrag);
  };

  const activeTab = tabs.find(t => t.id === activeTabId) || tabs[0];

  const updateTab = useCallback((id, updates) => {
    setTabs(prev => prev.map(t => t.id === id ? { ...t, ...updates } : t));
  }, []);

  const addNewTab = () => {
    const newId = Math.max(...tabs.map(t => t.id), 0) + 1;
    setTabs([...tabs, { id: newId, title: `SQL Query ${newId}`, query: '', notice: 'Yeni sekme.', isRunning: false, queryResult: null, queryError: '', explainResult: null, explainError: '' }]);
    setActiveTabId(newId);
  };

  const closeTab = (id, e) => {
    e.stopPropagation();
    if (tabs.length === 1) {
      updateTab(id, { query: '', notice: 'Temizlendi.', queryResult: null, queryError: '', explainResult: null, explainError: '' });
      return;
    }
    const newTabs = tabs.filter(t => t.id !== id);
    setTabs(newTabs);
    if (activeTabId === id) setActiveTabId(newTabs[0].id);
  };

  const formatQuery = useCallback(() => {
    try {
      const formatted = format(activeTab.query, { language: 'mysql', keywordCase: 'upper' });
      updateTab(activeTabId, { query: formatted, notice: 'Kod düzenlendi.' });
    } catch (err) {
      updateTab(activeTabId, { notice: 'Formatlama hatası: sözdizimi geçersiz olabilir.' });
    }
  }, [activeTab.query, activeTabId, updateTab]);

  const runQuery = useCallback(async () => {
    const executableSql = activeTab.query.replace(/^\s*--.*$/gm, '').trim();
    if (!executableSql) {
      updateTab(activeTabId, { notice: 'Çalıştırmak için geçerli bir SQL sorgusu yazın.' });
      return;
    }
    updateTab(activeTabId, { isRunning: true, queryError: '', explainError: '' });
    try {
      const result = await executeQuery(connectionToken, executableSql);
      updateTab(activeTabId, { queryResult: result, notice: `${result.executionTimeMs} ms içinde tamamlandı.` });
      try {
        const expResult = await explainQuery(connectionToken, executableSql);
        updateTab(activeTabId, { explainResult: expResult });
      } catch (expError) {
        updateTab(activeTabId, { explainError: expError.response?.data?.message || 'Açıklama alınamadı.' });
      }
    } catch (requestError) {
      const message = requestError.response?.data?.message || 'Sorgu çalıştırılamadı.';
      updateTab(activeTabId, { queryError: message, notice: 'Sorgu hatayla tamamlandı.' });
    } finally {
      updateTab(activeTabId, { isRunning: false });
    }
  }, [connectionToken, activeTab.query, activeTabId, updateTab]);

  const handleEditorKeydown = useCallback((event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      runQuery();
    }
    if (event.shiftKey && event.altKey && (event.key === 'f' || event.key === 'F')) {
      event.preventDefault();
      formatQuery();
    }
  }, [runQuery, formatQuery]);

  useEffect(() => {
    if (connectionToken) {
      manageTransaction(connectionToken, autoCommit ? 'autocommit_on' : 'autocommit_off')
        .then(() => updateTab(activeTabId, { notice: autoCommit ? 'Auto-Commit AÇIK' : 'Auto-Commit KAPALI.' }))
        .catch(() => updateTab(activeTabId, { notice: 'Auto-Commit durumu değiştirilemedi.' }));
    }
  }, [autoCommit, connectionToken, activeTabId, updateTab]);

  const runCommit = async () => {
    try {
      await manageTransaction(connectionToken, 'commit');
      updateTab(activeTabId, { notice: 'Değişiklikler başarıyla kaydedildi (Commit).' });
    } catch {
      updateTab(activeTabId, { notice: 'Commit işlemi başarısız oldu.' });
    }
  };

  const runRollback = async () => {
    try {
      await manageTransaction(connectionToken, 'rollback');
      updateTab(activeTabId, { notice: 'Değişiklikler geri alındı (Rollback).' });
    } catch {
      updateTab(activeTabId, { notice: 'Rollback işlemi başarısız oldu.' });
    }
  };

  const saveScript = useCallback(() => {
    const executableSql = activeTab.query.trim();
    if (!executableSql) {
      updateTab(activeTabId, { notice: 'Kaydedilecek sorgu boş.' });
      return;
    }
    const name = window.prompt('Bu betik için bir ad girin:');
    if (!name) return;
    const existing = JSON.parse(localStorage.getItem('savedScripts') || '[]');
    existing.push({ name, query: executableSql, id: Date.now(), database: currentDatabase });
    localStorage.setItem('savedScripts', JSON.stringify(existing));
    window.dispatchEvent(new Event('savedScriptsUpdated'));
    updateTab(activeTabId, { notice: `Betik '${name}' olarak kaydedildi.` });
  }, [activeTab.query, activeTabId, currentDatabase, updateTab]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', width: '100%' }}>
      <section className="sql-editor" aria-label="SQL editörü" style={{ display: 'flex', flexDirection: 'column', height: `${editorHeight}px`, flex: 'none', minHeight: 100 }}>
        <header className="editor-toolbar">
          <div className="editor-tabs">
            {tabs.map(tab => (
              <div key={tab.id} className={`editor-tab ${tab.id === activeTabId ? 'active' : ''}`} onClick={() => setActiveTabId(tab.id)}>
                <FiCode /> {tab.title} <button type="button" onClick={(e) => closeTab(tab.id, e)} title="Sekmeyi kapat" aria-label="Sekmeyi kapat"><FiX /></button>
              </div>
            ))}
            <button className="new-tab-button" type="button" onClick={addNewTab} title="Yeni sekme aç">+</button>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <button type="button" onClick={saveScript} title="Sorguyu Kaydet" style={{ background: 'transparent', border: '1px solid var(--border-muted)', color: 'var(--text-secondary)', borderRadius: '4px', padding: '4px 8px', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', cursor: 'pointer' }}><FiCheck /> Kaydet</button>
            <button type="button" onClick={formatQuery} title="Kodu Düzenle (Format)" style={{ background: 'transparent', border: '1px solid var(--border-muted)', color: 'var(--text-secondary)', borderRadius: '4px', padding: '4px 8px', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', cursor: 'pointer' }}><FiAlignLeft /> Formatla</button>
            <label style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', color: 'var(--text-secondary)', cursor: userRole === 'READ_ONLY' ? 'not-allowed' : 'pointer', opacity: userRole === 'READ_ONLY' ? 0.5 : 1 }}>
              <input type="checkbox" checked={autoCommit} onChange={(e) => setAutoCommit(e.target.checked)} disabled={userRole === 'READ_ONLY'} />
              Auto-Commit
            </label>
            <button type="button" onClick={runCommit} disabled={userRole === 'READ_ONLY' || autoCommit} title="Commit" style={{ background: 'transparent', border: '1px solid var(--border-muted)', color: 'var(--text-secondary)', borderRadius: '4px', padding: '4px 8px', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', cursor: (userRole === 'READ_ONLY' || autoCommit) ? 'not-allowed' : 'pointer' }}><FiCheck /> Commit</button>
            <button type="button" onClick={runRollback} disabled={userRole === 'READ_ONLY' || autoCommit} title="Rollback" style={{ background: 'transparent', border: '1px solid var(--border-muted)', color: 'var(--text-secondary)', borderRadius: '4px', padding: '4px 8px', display: 'flex', alignItems: 'center', gap: '4px', fontSize: '12px', cursor: (userRole === 'READ_ONLY' || autoCommit) ? 'not-allowed' : 'pointer' }}><FiRotateCcw /> Rollback</button>
            <button className="run-query-button" type="button" onClick={runQuery} disabled={activeTab.isRunning} title="Sorguyu çalıştır (Ctrl + Enter)"><FiPlay /> {activeTab.isRunning ? 'Çalışıyor…' : 'Çalıştır'} <kbd>Ctrl ↵</kbd></button>
          </div>
        </header>
        <div className="editor-content" onKeyDown={handleEditorKeydown}>
          <CodeMirror
            value={activeTab.query}
            height="100%"
            theme={isLightMode ? 'light' : oneDark}
            extensions={[sql({ dialect: MySQL })]}
            onChange={(val) => updateTab(activeTabId, { query: val })}
            basicSetup={{ lineNumbers: true, highlightActiveLine: true, autocompletion: true, bracketMatching: true, foldGutter: true }}
          />
        </div>
        <footer className="editor-statusbar">
          <span>{activeTab.notice}</span><span>MySQL · UTF-8</span>
        </footer>
      </section>

      {/* Resizer Handle */}
      <div 
        onMouseDown={startDrag}
        style={{ 
          height: '6px', 
          cursor: 'row-resize', 
          background: 'var(--bg-card)', 
          borderTop: '1px solid var(--border-subtle)',
          borderBottom: '1px solid var(--border-subtle)',
          zIndex: 10,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          transition: 'background 0.2s'
        }}
        onMouseEnter={(e) => e.target.style.background = 'var(--accent)'}
        onMouseLeave={(e) => e.target.style.background = 'var(--bg-card)'}
      >
        <div style={{ width: '40px', height: '2px', background: 'var(--border-subtle)', borderRadius: '2px' }} />
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        <QueryResults 
          result={activeTab.queryResult} 
          error={activeTab.queryError} 
          explainResult={activeTab.explainResult}
          explainError={activeTab.explainError}
          isRunning={activeTab.isRunning} 
          connectionToken={connectionToken} 
        />
      </div>
    </div>
  );
});

export default SqlEditor;
