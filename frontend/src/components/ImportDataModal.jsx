import React, { useState } from 'react';
import { FiX, FiUpload, FiArrowRight, FiCheckCircle, FiAlertCircle } from 'react-icons/fi';
import * as XLSX from 'xlsx';
import { executeQuery } from '../api/queryApi';

export default function ImportDataModal({ tableName, columns, connectionToken, onClose, onImported }) {
  const [step, setStep] = useState(1);
  const [file, setFile] = useState(null);
  const [fileData, setFileData] = useState([]);
  const [fileColumns, setFileColumns] = useState([]);
  const [mapping, setMapping] = useState({});
  const [isImporting, setIsImporting] = useState(false);
  const [progress, setProgress] = useState({ current: 0, total: 0 });
  const [error, setError] = useState('');

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    if (!selectedFile) return;
    setFile(selectedFile);
    setError('');
    
    const reader = new FileReader();
    reader.onload = (evt) => {
      try {
        const bstr = evt.target.result;
        const wb = XLSX.read(bstr, { type: 'binary' });
        const wsname = wb.SheetNames[0];
        const ws = wb.Sheets[wsname];
        const data = XLSX.utils.sheet_to_json(ws, { header: 1, defval: null });
        
        if (data.length < 2) {
            setError("Dosyada yeterli veri yok (başlık ve en az 1 satır olmalı).");
            return;
        }
        
        const fCols = data[0].map((c, i) => c ? String(c).trim() : 'Kolon ' + (i+1));
        const fData = data.slice(1).filter(row => row.some(cell => cell !== null && cell !== ''));
        
        setFileColumns(fCols);
        setFileData(fData);
        
        const initialMapping = {};
        columns.forEach(dbCol => {
            const match = fCols.find(fc => fc.toLowerCase() === dbCol.toLowerCase());
            initialMapping[dbCol] = match || '';
        });
        
        setMapping(initialMapping);
        setStep(2);
      } catch (err) {
          setError("Dosya okunamadı: " + err.message);
      }
    };
    reader.readAsBinaryString(selectedFile);
  };

  const handleImport = async () => {
    setIsImporting(true);
    setError('');
    
    const dbColToFileIndex = {};
    Object.entries(mapping).forEach(([dbCol, fCol]) => {
        if (fCol) {
            dbColToFileIndex[dbCol] = fileColumns.indexOf(fCol);
        }
    });
    
    const targetDbCols = Object.keys(dbColToFileIndex);
    if (targetDbCols.length === 0) {
        setError("En az bir kolon eşleştirilmelidir.");
        setIsImporting(false);
        return;
    }
    
    const tableParts = tableName.split('.');
    const formattedTableName = tableParts.map(part => `\`${part}\``).join('.');
    
    const BATCH_SIZE = 500;
    const totalRows = fileData.length;
    setProgress({ current: 0, total: totalRows });
    
    try {
        for (let i = 0; i < totalRows; i += BATCH_SIZE) {
            const batch = fileData.slice(i, i + BATCH_SIZE);
            let sql = `INSERT INTO ${formattedTableName} (${targetDbCols.map(c => `\`${c}\``).join(', ')}) VALUES `;
            
            const valueStrings = batch.map(row => {
                const vals = targetDbCols.map(dbCol => {
                    const idx = dbColToFileIndex[dbCol];
                    const val = row[idx];
                    if (val === undefined || val === null || val === '') return 'NULL';
                    return `'${String(val).replace(/'/g, "''")}'`;
                });
                return `(${vals.join(', ')})`;
            });
            
            sql += valueStrings.join(', ') + ';';
            
            const res = await executeQuery(connectionToken, sql);
            if (res && res.message && res.message.startsWith('Sorgu hatası')) {
                 throw new Error(res.message);
            }
            setProgress({ current: Math.min(i + BATCH_SIZE, totalRows), total: totalRows });
        }
        
        if (window.__triggerSchemaRefresh) window.__triggerSchemaRefresh();
        onImported();
        onClose();
    } catch (err) {
        setError(`${progress.current} satır eklendikten sonra hata oluştu: ` + (err.message || 'Bilinmeyen hata'));
        setIsImporting(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '600px', width: '100%', display: 'flex', flexDirection: 'column' }}>
        <div className="modal-header">
          <h3>İçe Aktar (Import): {tableName}</h3>
          <button className="close-btn" onClick={onClose}><FiX size={20} /></button>
        </div>
        
        <div className="modal-body" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {error && (
                <div className="error-message" style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#ef4444', background: 'rgba(239, 68, 68, 0.1)', padding: '12px', borderRadius: '4px' }}>
                    <FiAlertCircle /> <span>{error}</span>
                </div>
            )}
            
            {step === 1 && (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px', padding: '40px', border: '2px dashed var(--border-muted)', borderRadius: '8px' }}>
                    <FiUpload size={48} color="var(--text-muted)" />
                    <p style={{ color: 'var(--text-secondary)', textAlign: 'center' }}>.csv veya .xlsx dosyanızı seçin</p>
                    <label style={{ background: 'var(--accent)', color: '#fff', padding: '8px 16px', borderRadius: '4px', cursor: 'pointer', fontWeight: 500 }}>
                        Dosya Seç
                        <input type="file" accept=".csv, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/vnd.ms-excel" onChange={handleFileChange} style={{ display: 'none' }} />
                    </label>
                </div>
            )}

            {step === 2 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <h4 style={{ margin: 0 }}>Kolon Eşleştirme</h4>
                        <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>{fileData.length} Satır Bulundu</span>
                    </div>
                    
                    <div style={{ maxHeight: '300px', overflowY: 'auto', border: '1px solid var(--border-subtle)', borderRadius: '4px' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                            <thead style={{ background: 'var(--bg-layer-2)', position: 'sticky', top: 0 }}>
                                <tr>
                                    <th style={{ padding: '10px', textAlign: 'left', borderBottom: '1px solid var(--border-subtle)', fontSize: '13px' }}>Veritabanı Kolonu (Hedef)</th>
                                    <th style={{ padding: '10px', textAlign: 'left', borderBottom: '1px solid var(--border-subtle)', fontSize: '13px' }}>Dosya Kolonu (Kaynak)</th>
                                </tr>
                            </thead>
                            <tbody>
                                {columns.map(dbCol => (
                                    <tr key={dbCol} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                                        <td style={{ padding: '10px', fontSize: '13px', fontWeight: 500 }}>{dbCol}</td>
                                        <td style={{ padding: '10px' }}>
                                            <select 
                                                value={mapping[dbCol]} 
                                                onChange={(e) => setMapping({...mapping, [dbCol]: e.target.value})}
                                                style={{ width: '100%', padding: '6px', borderRadius: '4px', border: '1px solid var(--border-muted)', background: 'var(--bg-card)', color: 'var(--text-main)', fontSize: '13px' }}
                                                disabled={isImporting}
                                            >
                                                <option value="">-- Atla (Boş Bırak) --</option>
                                                {fileColumns.map(fCol => (
                                                    <option key={fCol} value={fCol}>{fCol}</option>
                                                ))}
                                            </select>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    
                    {isImporting && (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                                <span>İçe Aktarılıyor...</span>
                                <span>{progress.current} / {progress.total}</span>
                            </div>
                            <div style={{ width: '100%', height: '8px', background: 'var(--bg-layer-2)', borderRadius: '4px', overflow: 'hidden' }}>
                                <div style={{ width: `${(progress.current / progress.total) * 100}%`, height: '100%', background: 'var(--accent)', transition: 'width 0.2s' }}></div>
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
        
        {step === 2 && (
            <div className="modal-footer" style={{ padding: '16px 20px', borderTop: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button type="button" onClick={onClose} className="btn-secondary" disabled={isImporting}>İptal</button>
              <button type="button" onClick={handleImport} className="btn-primary" disabled={isImporting} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  {isImporting ? 'Aktarılıyor...' : <><FiCheckCircle /> İçe Aktar</>}
              </button>
            </div>
        )}
      </div>
    </div>
  );
}