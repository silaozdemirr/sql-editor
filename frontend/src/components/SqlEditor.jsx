import { useCallback, useState } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { sql, MySQL } from '@codemirror/lang-sql';
import { oneDark } from '@codemirror/theme-one-dark';
import { FiCode, FiPlay, FiX } from 'react-icons/fi';
import { executeQuery } from '../api/queryApi';

const INITIAL_SQL = `-- Sorgunuzu buraya yazın
SELECT *
FROM ogrenciler
LIMIT 25;`;

export default function SqlEditor({ sessionId, onQueryResult, onQueryError, onRunningChange }) {
  const [query, setQuery] = useState(INITIAL_SQL);
  const [notice, setNotice] = useState('Sorguyu çalıştırmak için Ctrl + Enter kullanın.');
  const [isRunning, setIsRunning] = useState(false);

  const runQuery = useCallback(async () => {
    const executableSql = query.replace(/^\s*--.*$/gm, '').trim();
    if (!executableSql) {
      setNotice('Çalıştırmak için geçerli bir SQL sorgusu yazın.');
      return;
    }
    setIsRunning(true); onRunningChange(true); onQueryError('');
    try {
      const result = await executeQuery(sessionId, executableSql);
      onQueryResult(result);
      setNotice(`${result.executionTimeMs} ms içinde tamamlandı.`);
    } catch (requestError) {
      const message = requestError.response?.data?.message || 'Sorgu çalıştırılamadı.';
      onQueryError(message); setNotice('Sorgu hatayla tamamlandı.');
    } finally {
      setIsRunning(false); onRunningChange(false);
    }
  }, [onQueryError, onQueryResult, onRunningChange, query, sessionId]);

  const handleEditorKeydown = useCallback((event) => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      runQuery();
    }
  }, [runQuery]);

  return (
    <section className="sql-editor" aria-label="SQL editörü">
      <header className="editor-toolbar">
        <div className="editor-tabs">
          <div className="editor-tab active"><FiCode /> SQL Query 1 <button type="button" title="Sekmeyi kapat" aria-label="Sekmeyi kapat"><FiX /></button></div>
          <button className="new-tab-button" type="button" disabled title="Çoklu sekme Aşama 4 sonrası eklenecek">+</button>
        </div>
        <button className="run-query-button" type="button" onClick={runQuery} disabled={isRunning} title="Sorguyu çalıştır (Ctrl + Enter)"><FiPlay /> {isRunning ? 'Çalışıyor…' : 'Çalıştır'} <kbd>Ctrl ↵</kbd></button>
      </header>
      <div className="editor-content" onKeyDown={handleEditorKeydown}>
        <CodeMirror
          value={query}
          height="100%"
          theme={oneDark}
          extensions={[sql({ dialect: MySQL })]}
          onChange={setQuery}
          basicSetup={{ lineNumbers: true, highlightActiveLine: true, autocompletion: true, bracketMatching: true, foldGutter: true }}
        />
      </div>
      <footer className="editor-statusbar">
        <span>{notice}</span><span>MySQL · UTF-8</span>
      </footer>
    </section>
  );
}
