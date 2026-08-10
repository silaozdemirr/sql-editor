import { FiAlertCircle, FiCheckCircle, FiDatabase } from 'react-icons/fi';

export default function QueryResults({ result, error, isRunning }) {
  const hasRows = result?.columns?.length > 0;
  return <section className="query-results" aria-label="Sorgu sonuçları">
    <header className="results-header"><div><FiDatabase /> Sonuçlar</div>{result && <span>{result.executionTimeMs} ms</span>}</header>
    {isRunning && <div className="results-state">Sorgu çalıştırılıyor…</div>}
    {!isRunning && error && <div className="results-state result-error"><FiAlertCircle /> {error}</div>}
    {!isRunning && !error && !result && <div className="results-state">Sorgu sonucunuz burada tablo olarak görüntülenecek.</div>}
    {!isRunning && !error && result && !hasRows && <div className="results-state result-success"><FiCheckCircle /> {result.message}</div>}
    {!isRunning && !error && hasRows && <div className="results-table-wrap"><table className="results-table">
      <thead><tr><th>#</th>{result.columns.map((column) => <th key={column}>{column}</th>)}</tr></thead>
      <tbody>{result.rows.map((row, rowIndex) => <tr key={rowIndex}><td>{rowIndex + 1}</td>{row.map((value, columnIndex) => <td key={`${rowIndex}-${columnIndex}`} title={value ?? 'NULL'} className={value === null ? 'null-value' : ''}>{value ?? 'NULL'}</td>)}</tr>)}</tbody>
    </table>{result.truncated && <p className="results-limit">İlk 1000 satır gösteriliyor. Sonucu daraltmak için LIMIT kullanın.</p>}</div>}
  </section>;
}
