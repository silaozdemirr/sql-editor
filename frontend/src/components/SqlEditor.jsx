import React, { useCallback, useState, useEffect, useRef, forwardRef, useImperativeHandle } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { sql, MySQL, PostgreSQL, MSSQL, SQLite, MariaSQL, PLSQL, StandardSQL } from '@codemirror/lang-sql';
import { oneDark } from '@codemirror/theme-one-dark';
import { FiCode, FiPlay, FiX, FiCheck, FiRotateCcw, FiAlignLeft, FiCpu, FiMessageSquare } from 'react-icons/fi';
import { format } from 'sql-formatter';
import { executeQuery, explainQuery, manageTransaction, generateSqlWithAi } from '../api/queryApi';
import QueryResults from './QueryResults';

const INITIAL_SQL = ``;

const SqlEditor = forwardRef(({ connectionToken, currentDatabase, userRole, dbType = 'MYSQL', tables = [] }, ref) => {
  const [tabs, setTabs] = useState([{
    id: 1, title: 'SQL Query 1', query: INITIAL_SQL, notice: 'Sorguyu çalıştırmak için Ctrl + Enter kullanın.', isRunning: false, queryResult: null, queryError: '', explainResult: null, explainError: ''
  }]);

  const getDialect = useCallback(() => {
    switch (dbType) {
      case 'POSTGRESQL': return PostgreSQL;
      case 'MSSQL': return MSSQL;
      case 'SQLITE': return SQLite;
      case 'MARIADB': return MariaSQL;
      case 'ORACLE': return PLSQL;
      case 'MYSQL': return MySQL;
      default: return StandardSQL;
    }
  }, [dbType]);

  const getFormatLanguage = useCallback(() => {
    switch (dbType) {
      case 'POSTGRESQL': return 'postgresql';
      case 'MSSQL': return 'tsql';
      case 'SQLITE': return 'sqlite';
      case 'MARIADB': return 'mariadb';
      case 'ORACLE': return 'plsql';
      case 'MYSQL': return 'mysql';
      default: return 'sql';
    }
  }, [dbType]);

  // Compute CodeMirror schema for autocompletion
  const sqlSchema = React.useMemo(() => {
    const s = {};
    tables.forEach(table => {
      s[table] = []; // Array of columns could go here if we fetched them
    });
    return s;
  }, [tables]);

  useImperativeHandle(ref, () => ({
    openTab: (title, query, autoRun = false) => {
      const id = Date.now();
      setTabs(prev => [...prev, {
        id, title, query, notice: '', isRunning: false, queryResult: null, queryError: '', explainResult: null, explainError: '', autoRunPending: autoRun
      }]);
      setActiveTabId(id);
    }
  }));
  const [activeTabId, setActiveTabId] = useState(1);
  const [autoCommit, setAutoCommit] = useState(true);
  const [editorHeight, setEditorHeight] = useState(300);
  const [isLightMode, setIsLightMode] = useState(() => document.body.classList.contains('light-mode'));
  
  // AI States
  const [aiPrompt, setAiPrompt] = useState('');
  const [aiGenerating, setAiGenerating] = useState(false);
  const [showAiSettings, setShowAiSettings] = useState(false);
  const [geminiApiKey, setGeminiApiKey] = useState(() => localStorage.getItem('geminiApiKey') || '');

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
      const formatted = format(activeTab.query, { language: getFormatLanguage(), keywordCase: 'upper' });
      updateTab(activeTabId, { query: formatted, notice: 'Kod düzenlendi.' });
    } catch (err) {
      updateTab(activeTabId, { notice: 'Formatlama hatası: sözdizimi geçersiz olabilir.' });
    }
  }, [activeTab.query, activeTabId, updateTab, getFormatLanguage]);

  const abortControllerRef = useRef(null);

  const handleAiGenerate = async (e) => {
    e.preventDefault();
    if (!geminiApiKey) {
      setShowAiSettings(true);
      return;
    }
    if (!aiPrompt.trim()) return;

    setAiGenerating(true);
    abortControllerRef.current = new AbortController();

    try {
      const result = await generateSqlWithAi(connectionToken, aiPrompt, dbType, geminiApiKey, abortControllerRef.current.signal);
      if (result && result.sql) {
        updateTab(activeTabId, { query: result.sql + '\n\n', notice: 'Yapay zeka kodu oluşturdu.' });
        setAiPrompt('');
      }
    } catch (err) {
      if (err.name === 'CanceledError' || err.code === 'ERR_CANCELED') {
        updateTab(activeTabId, { notice: 'Yapay zeka işlemi iptal edildi.' });
      } else {
        alert(err.response?.data?.message || 'Yapay zeka isteği başarısız oldu.');
      }
    } finally {
      setAiGenerating(false);
      abortControllerRef.current = null;
    }
  };

  const cancelAiGeneration = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
  };

  const editorRef = useRef(null);

  const runQuery = useCallback(async () => {
    let queryToRun = activeTab.query;
    if (editorRef.current && editorRef.current.view) {
      const view = editorRef.current.view;
      const selection = view.state.selection.main;
      if (!selection.empty) {
        queryToRun = view.state.sliceDoc(selection.from, selection.to);
      }
    }
    const executableSql = queryToRun.replace(/^\s*--.*$/gm, '').trim();
    
    if (!executableSql) {
      updateTab(activeTabId, { notice: 'Çalıştırmak için geçerli bir SQL sorgusu yazın veya seçin.' });
      return;
    }
    updateTab(activeTabId, { isRunning: true, queryError: '', explainError: '' });
    try {
      const result = await executeQuery(connectionToken, executableSql);
      
      // Backend returns 200 OK with error message starting with "Sorgu hatası:" to prevent console spam
      if (result && result.message && result.message.startsWith("Sorgu hatası:")) {
        let errorMsg = result.message;
        if (errorMsg.toLowerCase().includes("you have an error in your sql syntax") && executableSql.includes(";")) {
           errorMsg += "\n\nİPUCU: Birden fazla sorguyu aynı anda çalıştırırken hata alıyorsanız, lütfen çalıştırmak istediğiniz satırı veya bloğu fareyle seçip tek tek (Ctrl + Enter) çalıştırın.";
        }
        updateTab(activeTabId, { queryError: errorMsg, notice: 'Sorgu hatayla tamamlandı.' });
      } else {
        updateTab(activeTabId, { queryResult: result, notice: `${result.executionTimeMs} ms içinde tamamlandı.` });
      }
      
      try {
        const expResult = await explainQuery(connectionToken, executableSql);
        if (expResult && expResult.columns && expResult.columns.length > 0) {
          updateTab(activeTabId, { explainResult: expResult });
        } else {
          updateTab(activeTabId, { explainError: expResult?.message || 'Açıklama alınamadı.' });
        }
      } catch (expError) {
        updateTab(activeTabId, { explainError: expError.response?.data?.message || 'Açıklama alınamadı.' });
      }
    } catch (requestError) {
        let message = requestError.response?.data?.message || requestError.message || 'Sorgu çalıştırılamadı.';
        if (typeof message !== 'string') {
          try { message = JSON.stringify(message); } catch (e) { message = String(message); }
        }
        // If it's a MySQL multiple query error, give a helpful tip
        if (message.toLowerCase().includes("you have an error in your sql syntax") && executableSql.includes(";")) {
           message += "\n\nİPUCU: Birden fazla sorguyu aynı anda çalıştırırken hata alıyorsanız, lütfen sorguları fareyle seçip tek tek (Ctrl + Enter) çalıştırın veya sadece çalıştırmak istediğiniz satırı seçin.";
        }
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

  useEffect(() => {
    const active = tabs.find(t => t.id === activeTabId);
    if (active && active.autoRunPending && !active.isRunning) {
      updateTab(activeTabId, { autoRunPending: false });
      runQuery();
    }
  }, [tabs, activeTabId, runQuery, updateTab]);

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
        <div style={{ display: 'flex', padding: '8px 16px', background: 'var(--bg-layer-2)', borderBottom: '1px solid var(--border-subtle)', alignItems: 'center', gap: '8px' }}>
          <FiCpu color="var(--accent)" />
          <form onSubmit={handleAiGenerate} style={{ display: 'flex', flex: 1, gap: '8px' }}>
            <input 
              type="text" 
              value={aiPrompt}
              onChange={(e) => setAiPrompt(e.target.value)}
              placeholder="A.I.'a ne yapmak istediğinizi söyleyin... (Örn: En yüksek not alan 5 öğrenciyi getir)" 
              style={{ flex: 1, padding: '6px 12px', background: 'var(--bg-card)', border: '1px solid var(--border-muted)', borderRadius: '4px', color: 'var(--text-primary)' }}
              disabled={aiGenerating}
            />
            <button type="submit" disabled={aiGenerating || !aiPrompt.trim()} style={{ background: 'var(--accent)', color: 'white', border: 'none', padding: '6px 12px', borderRadius: '4px', cursor: aiGenerating || !aiPrompt.trim() ? 'not-allowed' : 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <FiMessageSquare /> {aiGenerating ? 'Düşünüyor...' : 'Üret'}
            </button>
            {aiGenerating && (
              <button type="button" onClick={cancelAiGeneration} style={{ background: '#e53e3e', color: 'white', border: 'none', padding: '6px 12px', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <FiX /> İptal
              </button>
            )}
            <button type="button" onClick={() => setShowAiSettings(true)} style={{ background: 'transparent', border: '1px solid var(--border-muted)', color: 'var(--text-secondary)', padding: '6px 12px', borderRadius: '4px', cursor: 'pointer' }}>
              Ayarlar
            </button>
          </form>
        </div>
        <div className="editor-content" onKeyDown={handleEditorKeydown}>
          <CodeMirror
            ref={editorRef}
            value={activeTab.query}
            theme={isLightMode ? 'light' : oneDark}
            extensions={[sql({ dialect: getDialect(), schema: sqlSchema })]}
            onChange={(val) => updateTab(activeTabId, { query: val })}
            basicSetup={{ lineNumbers: true, highlightActiveLine: true, autocompletion: true, bracketMatching: true, foldGutter: true }}
          />
        </div>
        <footer className="editor-statusbar">
          <span>{activeTab.notice}</span><span>{dbType} · UTF-8</span>
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

      {showAiSettings && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', zIndex: 9999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: 'var(--bg-card)', padding: '24px', borderRadius: '8px', width: '400px', border: '1px solid var(--border-subtle)', boxShadow: '0 4px 12px rgba(0,0,0,0.2)' }}>
            <h3 style={{ marginTop: 0, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}><FiCpu /> Yapay Zeka Ayarları</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '16px', lineHeight: 1.5 }}>
              Doğal dilden SQL üretmek için ücretsiz bir <strong>Google Gemini API Anahtarı</strong> gereklidir. <a href="https://aistudio.google.com/" target="_blank" rel="noreferrer" style={{ color: 'var(--accent)' }}>aistudio.google.com</a> adresinden ücretsiz alabilirsiniz. Bu anahtar sadece sizin tarayıcınızda (yerel) saklanır ve doğrudan Gemini'ye iletilir.
            </p>
            <input 
              type="password" 
              value={geminiApiKey}
              onChange={e => setGeminiApiKey(e.target.value)}
              placeholder="AIzaSy..."
              style={{ width: '100%', padding: '8px 12px', marginBottom: '16px', border: '1px solid var(--border-muted)', background: 'var(--bg-layer-2)', color: 'var(--text-primary)', borderRadius: '4px' }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button type="button" onClick={() => setShowAiSettings(false)} style={{ background: 'transparent', border: '1px solid var(--border-muted)', color: 'var(--text-primary)', padding: '6px 16px', borderRadius: '4px', cursor: 'pointer' }}>İptal</button>
              <button type="button" onClick={() => {
                localStorage.setItem('geminiApiKey', geminiApiKey);
                setShowAiSettings(false);
              }} style={{ background: 'var(--accent)', border: 'none', color: 'white', padding: '6px 16px', borderRadius: '4px', cursor: 'pointer' }}>Kaydet</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
});

export default SqlEditor;
