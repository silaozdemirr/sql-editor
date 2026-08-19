import React, { useState, useMemo, useRef, useEffect } from 'react';
import { FiAlertCircle, FiCheckCircle, FiDatabase, FiClock, FiInfo, FiDownload, FiPlus, FiMinus, FiPieChart } from 'react-icons/fi';
import { getQueryHistory, updateCell, executeQuery } from '../api/queryApi';
import { AgGridReact } from 'ag-grid-react';
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-quartz.css';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

ModuleRegistry.registerModules([AllCommunityModule]);

const ChartRenderer = ({ data, columns }) => {
  const [xAxis, setXAxis] = useState(columns[0] || '');
  const [yAxis, setYAxis] = useState(columns[1] || columns[0] || '');
  const [chartType, setChartType] = useState('bar'); 
  
  if (!data || data.length === 0) return <div style={{ padding: '20px', color: 'var(--text-muted)' }}>Grafik çizilecek veri bulunamadı.</div>;

  // Recharts needs actual numbers to calculate pie angles and bar heights
  const parsedData = useMemo(() => {
    return data.map(row => {
      const newRow = { ...row };
      for (const key in newRow) {
        if (typeof newRow[key] === 'string' && !isNaN(Number(newRow[key])) && newRow[key] !== '') {
          newRow[key] = Number(newRow[key]);
        }
      }
      return newRow;
    });
  }, [data]);

  const numericColumns = columns; // Allow picking any column, Recharts attempts to parse numbers anyway
  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#a4de6c', '#d0ed57', '#8884d8', '#8dd1e1'];

  return (
    <div style={{ padding: '20px', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', gap: '15px', marginBottom: '20px' }}>
         <select className="form-input" style={{ width: 'auto', padding: '6px 12px' }} value={chartType} onChange={e => setChartType(e.target.value)}>
           <option value="bar">Sütun Grafiği (Bar)</option>
           <option value="pie">Pasta Grafiği (Pie)</option>
         </select>
         <select className="form-input" style={{ width: 'auto', padding: '6px 12px' }} value={xAxis} onChange={e => setXAxis(e.target.value)}>
           {columns.map(c => <option key={c} value={c}>{c} (Kategori/X)</option>)}
         </select>
         <select className="form-input" style={{ width: 'auto', padding: '6px 12px' }} value={yAxis} onChange={e => setYAxis(e.target.value)}>
           {numericColumns.map(c => <option key={c} value={c}>{c} (Değer/Y)</option>)}
         </select>
      </div>
      <div style={{ flex: 1, minHeight: 0 }}>
        <ResponsiveContainer width="100%" height="100%">
          {chartType === 'bar' ? (
            <BarChart data={parsedData} margin={{ top: 20, right: 30, left: 20, bottom: 50 }}>
              <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
              <XAxis dataKey={xAxis} angle={-45} textAnchor="end" height={80} stroke="var(--text-muted)" />
              <YAxis stroke="var(--text-muted)" />
              <RechartsTooltip contentStyle={{ backgroundColor: 'var(--bg-layer-2)', borderColor: 'var(--border-subtle)' }} itemStyle={{ color: 'var(--text-primary)' }} />
              <Legend />
              <Bar dataKey={yAxis} fill="#3b82f6" name={yAxis} />
            </BarChart>
          ) : (
            <PieChart>
              <Pie data={parsedData} dataKey={yAxis} nameKey={xAxis} cx="50%" cy="50%" outerRadius={120} label>
                {parsedData.map((entry, index) => <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />)}
              </Pie>
              <RechartsTooltip contentStyle={{ backgroundColor: 'var(--bg-layer-2)', borderColor: 'var(--border-subtle)' }} itemStyle={{ color: 'var(--text-primary)' }} />
              <Legend />
            </PieChart>
          )}
        </ResponsiveContainer>
      </div>
    </div>
  );
};

const formatDataForGrid = (data, isEditable = false) => {
  if (!data || !data.columns || !data.rows) return { rowData: [], colDefs: [] };
  const colDefs = data.columns.map((col) => ({ 
    field: col, 
    headerName: col,
    editable: isEditable,
    valueFormatter: (params) => params.value === null ? 'NULL' : params.value,
    cellClass: (params) => params.value === null ? 'null-value' : (isEditable ? 'editable-cell' : ''),
    comparator: (valueA, valueB) => {
      if (valueA === null && valueB === null) return 0;
      if (valueA === null) return -1;
      if (valueB === null) return 1;
      const numA = Number(valueA);
      const numB = Number(valueB);
      if (!isNaN(numA) && !isNaN(numB) && valueA !== '' && valueB !== '') {
         return numA - numB;
      }
      return String(valueA).localeCompare(String(valueB));
    }
  }));
  
  colDefs.unshift({
      headerName: '#',
      valueGetter: 'node.rowIndex + 1',
      width: 70,
      minWidth: 70,
      maxWidth: 70,
      pinned: 'left',
      suppressMenu: true,
      filter: false,
      sortable: false,
      resizable: false
  });

  const rowData = data.rows.map((row) => {
    const obj = {};
    data.columns.forEach((col, i) => {
      obj[col] = row[i];
    });
    return obj;
  });
  return { rowData, colDefs };
};

export default function QueryResults({ result, error, explainResult, explainError, isRunning, connectionToken }) {
  const [activeTab, setActiveTab] = useState('results');
  const [history, setHistory] = useState(null);
  const [historyError, setHistoryError] = useState('');
  const [isLightMode, setIsLightMode] = useState(() => document.body.classList.contains('light-mode'));
  const gridRef = useRef();
  
  useEffect(() => {
    const handleThemeChange = () => setIsLightMode(document.body.classList.contains('light-mode'));
    window.addEventListener('themeChanged', handleThemeChange);
    return () => window.removeEventListener('themeChanged', handleThemeChange);
  }, []);

  const hasRows = result?.columns?.length > 0;
  
  const { rowData: resultRowData, colDefs: resultColDefs } = useMemo(() => formatDataForGrid(result, !!result?.tableName), [result]);
  const { rowData: explainRowData, colDefs: explainColDefs } = useMemo(() => formatDataForGrid(explainResult, false), [explainResult]);

  const handleCellValueChanged = async (params) => {
    if (!result?.tableName) return;
    const { colDef, newValue, oldValue, data } = params;
    
    if (newValue === oldValue) return;

    if (data._isNewRow) {
      const columns = [];
      const values = [];
      Object.keys(data).forEach(key => {
        if (key !== '_isNewRow' && data[key] !== undefined && data[key] !== null && data[key] !== '') {
          columns.push(`\`${key}\``);
          values.push(`'${String(data[key]).replace(/'/g, "''")}'`);
        }
      });
      
      if (columns.length === 0) return;

      try {
        const sql = `INSERT INTO \`${result.tableName}\` (${columns.join(', ')}) VALUES (${values.join(', ')})`;
        await executeQuery(connectionToken, sql);
        data._isNewRow = false;
        alert('Yeni satır başarıyla eklendi! Tüm verileri ve otomatik oluşan ID leri görmek için sorguyu yeniden çalıştırın.');
      } catch (err) {
        alert('Satır eklenirken hata: ' + (err.response?.data?.message || 'Zorunlu alanları doldurun.'));
      }
      return;
    }

    try {
      const oldRowValues = { ...data };
      oldRowValues[colDef.field] = oldValue;
      
      const res = await updateCell(result.tableName, colDef.field, newValue, oldRowValues, connectionToken);
      console.log("Update success:", res);
    } catch (err) {
      params.node.setDataValue(colDef.field, oldValue);
      alert(err.response?.data?.message || 'Hücre güncellenemedi.');
    }
  };

  const handleAddRow = () => {
    if (!result?.tableName) return;
    gridRef.current.api.applyTransaction({ add: [{ _isNewRow: true }] });
  };

  const handleDeleteRow = async () => {
    if (!result?.tableName) return;
    const selectedNodes = gridRef.current.api.getSelectedNodes();
    if (selectedNodes.length === 0) {
      alert('Lütfen silmek için bir satır seçin.');
      return;
    }
    const data = selectedNodes[0].data;
    if (!window.confirm('Seçili satırı silmek istediğinize emin misiniz?')) return;
    
    const conditions = [];
    result.columns.forEach(col => {
      const val = data[col];
      if (val === null || val === undefined) {
        conditions.push(`\`${col}\` IS NULL`);
      } else {
        conditions.push(`\`${col}\` = '${String(val).replace(/'/g, "''")}'`);
      }
    });
    
    const sql = `DELETE FROM \`${result.tableName}\` WHERE ${conditions.join(' AND ')} LIMIT 1`;
    try {
      await executeQuery(connectionToken, sql);
      gridRef.current.api.applyTransaction({ remove: [data] });
    } catch (err) {
      alert(err.response?.data?.message || 'Satır silinemedi.');
    }
  };

  const exportToCsv = () => {
    if (!gridRef.current || !gridRef.current.api) return;
    gridRef.current.api.exportDataAsCsv({ fileName: 'sorgu_sonucu.csv' });
  };

  const exportToExcel = async () => {
    if (!gridRef.current || !gridRef.current.api) return;
    try {
      // Dinamik import ile sadece ihtiyaç duyulduğunda yükle
      const XLSX = await import('xlsx');
      
      const rowData = [];
      gridRef.current.api.forEachNode((node) => {
        rowData.push(node.data);
      });
      
      // Sütun başlıklarını al (sadece görünür olanlar, veya hepsi)
      const cols = gridRef.current.api.getColumns()
        .filter(col => col.getColId() !== '0') // # kolonunu atla
        .map(col => col.getColDef().field);

      const worksheet = XLSX.utils.json_to_sheet(rowData, { header: cols });
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, "Sonuçlar");
      
      XLSX.writeFile(workbook, 'sorgu_sonucu.xlsx');
    } catch (err) {
      console.error('Excel olarak kaydedilemedi:', err);
      alert('Excel dosyası oluşturulurken bir hata oluştu.');
    }
  };

  const defaultColDef = useMemo(() => ({
    sortable: true,
    filter: true,
    resizable: true,
    minWidth: 100
  }), []);

  const loadHistory = async () => {
    try { setHistory(await getQueryHistory(connectionToken)); setHistoryError(''); } 
    catch (e) { setHistoryError(e.response?.data?.message || 'Geçmiş alınamadı.'); }
  };
  
  return <section className="query-results" aria-label="Sorgu sonuçları">
    <header className="results-header" style={{ display: 'flex', borderBottom: '1px solid var(--border-subtle)', padding: 0 }}>
      <div style={{ display: 'flex', gap: '16px', padding: '0 16px' }}>
        <button type="button" onClick={() => setActiveTab('results')} style={{ background: 'none', border: 'none', borderBottom: activeTab === 'results' ? '2px solid var(--accent)' : '2px solid transparent', padding: '8px 0', color: activeTab === 'results' ? 'var(--text-primary)' : 'var(--text-muted)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px', fontWeight: activeTab === 'results' ? 600 : 400 }}><FiDatabase /> Sonuçlar</button>
        <button type="button" onClick={() => { setActiveTab('history'); loadHistory(); }} style={{ background: 'none', border: 'none', borderBottom: activeTab === 'history' ? '2px solid var(--accent)' : '2px solid transparent', padding: '8px 0', color: activeTab === 'history' ? 'var(--text-primary)' : 'var(--text-muted)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px', fontWeight: activeTab === 'history' ? 600 : 400 }}><FiClock /> Sorgu Geçmişi</button>
        <button type="button" onClick={() => setActiveTab('explain')} style={{ background: 'none', border: 'none', borderBottom: activeTab === 'explain' ? '2px solid var(--accent)' : '2px solid transparent', padding: '8px 0', color: activeTab === 'explain' ? 'var(--text-primary)' : 'var(--text-muted)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px', fontWeight: activeTab === 'explain' ? 600 : 400 }}><FiInfo /> Açıklama</button>
        <button type="button" onClick={() => setActiveTab('chart')} style={{ background: 'none', border: 'none', borderBottom: activeTab === 'chart' ? '2px solid var(--accent)' : '2px solid transparent', padding: '8px 0', color: activeTab === 'chart' ? 'var(--text-primary)' : 'var(--text-muted)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px', fontWeight: activeTab === 'chart' ? 600 : 400 }}><FiPieChart /> Grafik</button>
      </div>
      <div style={{ marginLeft: 'auto', paddingRight: '16px', display: 'flex', alignItems: 'center', gap: '12px' }}>
        {activeTab === 'results' && hasRows && (
          <>
            {result?.tableName && (
              <>
                <button type="button" onClick={handleAddRow} style={{ padding: '5px 10px', fontSize: '11.5px', display: 'flex', alignItems: 'center', gap: '5px', background: 'rgba(59, 130, 246, 0.1)', border: '1px solid rgba(59, 130, 246, 0.25)', color: '#3b82f6', borderRadius: '4px', cursor: 'pointer', fontWeight: 500 }} title="Yeni boş satır ekle">
                  <FiPlus size={13} />
                </button>
                <button type="button" onClick={handleDeleteRow} style={{ padding: '5px 10px', fontSize: '11.5px', display: 'flex', alignItems: 'center', gap: '5px', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.25)', color: '#ef4444', borderRadius: '4px', cursor: 'pointer', fontWeight: 500 }} title="Seçili satırı sil">
                  <FiMinus size={13} />
                </button>
              </>
            )}
            <button type="button" onClick={exportToCsv} style={{ padding: '5px 10px', fontSize: '11.5px', display: 'flex', alignItems: 'center', gap: '5px', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.25)', color: '#10b981', borderRadius: '4px', cursor: 'pointer', fontWeight: 500 }} title="CSV olarak kaydet">
              <FiDownload size={13} />CSV
            </button>
            <button type="button" onClick={exportToExcel} style={{ padding: '5px 10px', fontSize: '11.5px', display: 'flex', alignItems: 'center', gap: '5px', background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.25)', color: '#10b981', borderRadius: '4px', cursor: 'pointer', fontWeight: 500 }} title="Excel olarak kaydet">
              <FiDownload size={13} />Excel
            </button>
          </>
        )}
        {result && <span style={{ color: '#aaa', fontSize: '12px' }}>{result.executionTimeMs} ms</span>}
      </div>
    </header>
    
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      {activeTab === 'results' && (
        <>
          {isRunning && <div className="results-state">Sorgu çalıştırılıyor…</div>}
          {!isRunning && error && <div className="results-state result-error"><FiAlertCircle /> {error}</div>}
          {!isRunning && !error && !result && <div className="results-state">Sorgu sonucunuz burada tablo olarak görüntülenecek.</div>}
          {!isRunning && !error && result && !hasRows && <div className="results-state result-success"><FiCheckCircle /> {result.message}</div>}
          {!isRunning && !error && hasRows && <div className={`results-table-wrap ag-theme-quartz ${!isLightMode ? 'ag-theme-quartz-dark' : ''}`} style={{ flex: 1, width: '100%', display: 'flex' }}>
            <AgGridReact 
              ref={gridRef}
              theme="legacy"
              rowData={resultRowData} 
              columnDefs={resultColDefs} 
              defaultColDef={defaultColDef} 
              pagination={true} 
              paginationPageSize={50}
              paginationPageSizeSelector={[20, 50, 100, 500]}
              rowHeight={35}
              headerHeight={40}
              rowSelection="single"
              onCellValueChanged={handleCellValueChanged}
              style={{ flex: 1, width: '100%', height: '100%' }}
            />
          </div>}
        </>
      )}
      {activeTab === 'history' && (
        <div className="results-table-wrap" style={{ padding: '16px', overflow: 'auto' }}>
          {!history && !historyError && <div className="results-state">Geçmiş yükleniyor…</div>}
          {historyError && <div className="results-state result-error"><FiAlertCircle /> {historyError}</div>}
          {history && history.length === 0 && <div className="results-state">Sorgu geçmişi boş.</div>}
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
      {activeTab === 'explain' && <div className="results-table-wrap" style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          {isRunning && <div className="results-state" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Açıklama planı çalıştırılıyor…</div>}
          {!isRunning && explainError && <div className="results-state result-error" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><FiAlertCircle /> {explainError}</div>}
          {!isRunning && !explainError && !explainResult && <div className="results-state" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#888' }}>
            Henüz bir sorgu çalıştırılmadı.
          </div>}
          {!isRunning && !explainError && explainResult && <div className={`ag-theme-quartz ${!isLightMode ? 'ag-theme-quartz-dark' : ''}`} style={{ flex: 1, width: '100%', display: 'flex' }}>
            <AgGridReact 
              theme="legacy"
              rowData={explainRowData} 
              columnDefs={explainColDefs} 
              defaultColDef={defaultColDef} 
              pagination={true} 
              paginationPageSize={50}
              rowHeight={35}
              headerHeight={40}
              style={{ flex: 1, width: '100%', height: '100%' }}
            />
          </div>}
      </div>}
      {activeTab === 'chart' && (
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          {isRunning && <div className="results-state">Sorgu çalıştırılıyor…</div>}
          {!isRunning && !result && <div className="results-state">Sorgu sonucu bulunamadı.</div>}
          {!isRunning && result && hasRows && <ChartRenderer data={resultRowData} columns={resultColDefs.map(c => c.field).filter(f => f !== undefined)} />}
          {!isRunning && result && !hasRows && <div className="results-state">Çizilecek tablo verisi yok.</div>}
        </div>
      )}
    </div>
  </section>;
}
