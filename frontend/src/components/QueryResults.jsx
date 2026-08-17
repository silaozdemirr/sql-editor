import { useState, useEffect } from 'react';
import { FiAlertCircle, FiCheckCircle, FiDatabase, FiClock, FiInfo, FiPlay } from 'react-icons/fi';
import { getQueryHistory } from '../api/queryApi';

export default function QueryResults({ result, error, explainResult, explainError, isRunning, connectionToken }) {
  const [activeTab, setActiveTab] = useState('results');
  const [history, setHistory] = useState(null);
  const [historyError, setHistoryError] = useState('');
  const hasRows = result?.columns?.length > 0;
  
  useEffect(() => {
    if (activeTab === 'history' && connectionToken) {
      getQueryHistory(connectionToken)
        .then(setHistory)
        .catch(e => setHistoryError(e.response?.data?.message || 'Geçmiş alınamadı.'));
    }
  }, [activeTab, connectionToken, result]);

  return <section className="query-results" aria-label="Sorgu sonuçları">
    <header className="results-header" style={{ display: 'flex', borderBottom: '1px solid #333', padding: 0 }}>
      <div style={{ display: 'flex', gap: '16px', padding: '0 16px' }}>
        <button type="button" onClick={() => setActiveTab('results')} style={{ background: 'none', border: 'none', borderBottom: activeTab === 'results' ? '2px solid #007acc' : '2px solid transparent', padding: '8px 0', color: activeTab === 'results' ? '#fff' : '#aaa', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}><FiDatabase /> Sonuçlar</button>
        <button type="button" onClick={() => setActiveTab('history')} style={{ background: 'none', border: 'none', borderBottom: activeTab === 'history' ? '2px solid #007acc' : '2px solid transparent', padding: '8px 0', color: activeTab === 'history' ? '#fff' : '#aaa', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}><FiClock /> Sorgu Geçmişi</button>
        <button type="button" onClick={() => setActiveTab('explain')} style={{ background: 'none', border: 'none', borderBottom: activeTab === 'explain' ? '2px solid #007acc' : '2px solid transparent', padding: '8px 0', color: activeTab === 'explain' ? '#fff' : '#aaa', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}><FiInfo /> Açıklama</button>
      </div>
      <div style={{ marginLeft: 'auto', paddingRight: '16px', display: 'flex', alignItems: 'center' }}>{result && <span>{result.executionTimeMs} ms</span>}</div>
    </header>
    
    <div style={{ flex: 1, overflow: 'auto', padding: activeTab === 'results' ? '0' : '16px' }}>
      {activeTab === 'results' && (
        <>
          {isRunning && <div className="results-state">Sorgu çalıştırılıyor…</div>}
          {!isRunning && error && <div className="results-state result-error"><FiAlertCircle /> {error}</div>}
          {!isRunning && !error && !result && <div className="results-state">Sorgu sonucunuz burada tablo olarak görüntülenecek.</div>}
          {!isRunning && !error && result && !hasRows && <div className="results-state result-success"><FiCheckCircle /> {result.message}</div>}
          {!isRunning && !error && hasRows && <div className="results-table-wrap"><table className="results-table">
            <thead><tr><th>#</th>{result.columns.map((column) => <th key={column}>{column}</th>)}</tr></thead>
            <tbody>{result.rows.map((row, rowIndex) => <tr key={rowIndex}><td>{rowIndex + 1}</td>{row.map((value, columnIndex) => <td key={`${rowIndex}-${columnIndex}`} title={value ?? 'NULL'} className={value === null ? 'null-value' : ''}>{value ?? 'NULL'}</td>)}</tr>)}</tbody>
          </table>{result.truncated && <p className="results-limit">İlk 1000 satır gösteriliyor. Sonucu daraltmak için LIMIT kullanın.</p>}</div>}
        </>
      )}
      {activeTab === 'history' && (
        <div className="results-table-wrap">
          {historyError && <div className="results-state result-error"><FiAlertCircle /> {historyError}</div>}
          {!historyError && !history && <div className="results-state">Yükleniyor...</div>}
          {!historyError && history && history.length === 0 && <div className="results-state">Henüz bir sorgu geçmişiniz yok.</div>}
          {history && history.length > 0 && <table className="results-table">
            <thead><tr><th>Zaman</th><th>Süre (ms)</th><th>Durum</th><th>Sorgu</th></tr></thead>
            <tbody>{history.map((h, i) => <tr key={i}>
              <td style={{ minWidth: '150px' }}>{new Date(h.created_at).toLocaleString()}</td>
              <td>{h.execution_time_ms}</td>
              <td style={{ color: h.status === 'SUCCESS' ? '#4ade80' : '#f87171' }}>{h.status}</td>
              <td style={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace' }}>{h.query_text}</td>
            </tr>)}</tbody>
          </table>}
        </div>
      )}
      {activeTab === 'explain' && (
        <div className="results-table-wrap" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
            {isRunning && <div className="results-state">Açıklama planı çalıştırılıyor…</div>}
            {!isRunning && explainError && <div className="results-state result-error"><FiAlertCircle /> {explainError}</div>}
            {!isRunning && !explainError && !explainResult && <div className="results-state" style={{ color: '#888' }}>
              Henüz bir sorgu çalıştırılmadı.
            </div>}
            {!isRunning && !explainError && explainResult && <table className="results-table" style={{ margin: 0 }}>
            <thead><tr><th>#</th>{explainResult.columns.map((column) => <th key={column}>{column}</th>)}</tr></thead>
            <tbody>{explainResult.rows.map((row, rowIndex) => <tr key={rowIndex}><td>{rowIndex + 1}</td>{row.map((value, columnIndex) => <td key={`${rowIndex}-${columnIndex}`} title={value ?? 'NULL'} className={value === null ? 'null-value' : ''}>{value ?? 'NULL'}</td>)}</tr>)}</tbody>
          </table>}
        </div>
      )}
    </div>
  </section>;
}
