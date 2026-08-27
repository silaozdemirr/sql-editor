import React, { useState } from 'react';
import { FiPlus, FiX, FiCheck, FiTrash2 } from 'react-icons/fi';
import { createTable } from '../api/schemaApi';

const DATA_TYPES = [
  'INT', 'BIGINT', 'VARCHAR(255)', 'VARCHAR(100)', 'TEXT', 'LONGTEXT', 
  'BOOLEAN', 'DATE', 'DATETIME', 'TIMESTAMP', 'DECIMAL(10,2)', 'FLOAT', 'DOUBLE'
];

export default function CreateTableModal({ database, onClose, onCreated }) {
  const [tableName, setTableName] = useState('');
  const [columns, setColumns] = useState([
    { id: 1, name: 'id', type: 'INT', primaryKey: true, autoIncrement: true, notNull: true }
  ]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const addColumn = () => {
    setColumns([...columns, { 
      id: Date.now(), name: '', type: 'VARCHAR(255)', primaryKey: false, autoIncrement: false, notNull: false 
    }]);
  };

  const updateColumn = (id, field, value) => {
    setColumns(columns.map(c => c.id === id ? { ...c, [field]: value } : c));
  };

  const removeColumn = (id) => {
    setColumns(columns.filter(c => c.id !== id));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!tableName.trim()) {
      setError('Tablo adı girilmelidir.');
      return;
    }
    if (columns.length === 0) {
      setError('En az bir kolon eklenmelidir.');
      return;
    }
    for (let c of columns) {
      if (!c.name.trim()) {
        setError('Tüm kolonların adı olmalıdır.');
        return;
      }
    }

    try {
      setIsSubmitting(true);
      setError('');
      await createTable({
        databaseName: database,
        tableName: tableName,
        columns: columns
      });
      onCreated();
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.7)', zIndex: 99999, display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(3px)' }}>
      <div style={{ background: 'var(--bg-card, #242424)', width: '95vw', maxWidth: '900px', borderRadius: '8px', display: 'flex', flexDirection: 'column', maxHeight: '90vh', boxShadow: '0 10px 25px rgba(0,0,0,0.8)', border: '1px solid var(--border-muted)' }}>
        <header style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>Yeni Tablo Oluştur - <span style={{color: 'var(--accent)'}}>{database}</span></h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-primary)', cursor: 'pointer' }}><FiX size={24} /></button>
        </header>
        
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1 }}>
          {error && <div className="schema-error" style={{ marginBottom: '16px', padding: '12px', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', borderRadius: '4px', border: '1px solid rgba(239, 68, 68, 0.2)' }}>{error}</div>}
          
          <div className="form-group">
            <label className="form-label" style={{ fontWeight: '500', marginBottom: '8px' }}>Tablo Adı</label>
            <input 
              className="form-input" 
              value={tableName} 
              onChange={e => setTableName(e.target.value)} 
              placeholder="Örn: kullanicilar" 
              style={{ padding: '10px', fontSize: '14px', width: '100%', maxWidth: '400px' }}
            />
          </div>

          <div style={{ marginTop: '28px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <h3 style={{ fontSize: '16px', margin: 0, fontWeight: '500' }}>Kolonlar</h3>
              <button className="btn btn-secondary btn-sm" onClick={addColumn} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}><FiPlus /> Kolon Ekle</button>
            </div>
            
            <div style={{ overflowX: 'auto' }}>
              <table className="admin-table" style={{ width: '100%', minWidth: '700px' }}>
                <thead>
                  <tr style={{ background: 'var(--bg-layer-2)' }}>
                    <th style={{ padding: '12px 8px' }}>Kolon Adı</th>
                    <th style={{ padding: '12px 8px' }}>Veri Tipi</th>
                    <th style={{ padding: '12px 8px', textAlign: 'center' }}>Birincil Anahtar (PK)</th>
                    <th style={{ padding: '12px 8px', textAlign: 'center' }}>Boş Olamaz (NN)</th>
                    <th style={{ padding: '12px 8px', textAlign: 'center' }}>Oto Artan (AI)</th>
                    <th style={{ width: '40px' }}></th>
                  </tr>
                </thead>
                <tbody>
                  {columns.map(c => (
                    <tr key={c.id}>
                      <td style={{ padding: '8px' }}>
                        <input className="form-input" style={{ width: '100%' }} value={c.name} onChange={e => updateColumn(c.id, 'name', e.target.value)} placeholder="kolon_adi" />
                      </td>
                      <td style={{ padding: '8px' }}>
                        <select className="form-input" style={{ width: '100%', cursor: 'pointer' }} value={c.type} onChange={e => updateColumn(c.id, 'type', e.target.value)}>
                          {DATA_TYPES.map(type => (
                            <option key={type} value={type}>{type}</option>
                          ))}
                        </select>
                      </td>
                      <td style={{ padding: '8px', textAlign: 'center' }}>
                        <select className="form-input" style={{ width: '70px', display: 'inline-block', cursor: 'pointer' }} value={c.primaryKey ? "1" : "0"} onChange={e => updateColumn(c.id, 'primaryKey', e.target.value === "1")}>
                          <option value="1">Evet</option>
                          <option value="0">Hayır</option>
                        </select>
                      </td>
                      <td style={{ padding: '8px', textAlign: 'center' }}>
                        <select className="form-input" style={{ width: '70px', display: 'inline-block', cursor: 'pointer' }} value={c.notNull ? "1" : "0"} onChange={e => updateColumn(c.id, 'notNull', e.target.value === "1")}>
                          <option value="1">Evet</option>
                          <option value="0">Hayır</option>
                        </select>
                      </td>
                      <td style={{ padding: '8px', textAlign: 'center' }}>
                        <select className="form-input" style={{ width: '70px', display: 'inline-block', cursor: 'pointer' }} value={c.autoIncrement ? "1" : "0"} onChange={e => updateColumn(c.id, 'autoIncrement', e.target.value === "1")}>
                          <option value="1">Evet</option>
                          <option value="0">Hayır</option>
                        </select>
                      </td>
                      <td style={{ padding: '8px', textAlign: 'center' }}>
                        <button style={{ color: '#ef4444', background: 'transparent', border: 'none', cursor: 'pointer', padding: '6px', borderRadius: '4px' }} onClick={() => removeColumn(c.id)} title="Kolonu Sil">
                          <FiTrash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        
        <footer style={{ padding: '16px 20px', borderTop: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'flex-end', gap: '12px', background: 'var(--bg-layer-2)', borderBottomLeftRadius: '8px', borderBottomRightRadius: '8px' }}>
          <button className="btn btn-secondary" onClick={onClose} disabled={isSubmitting}>İptal</button>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={isSubmitting} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <FiCheck /> {isSubmitting ? 'Oluşturuluyor...' : 'Tabloyu Oluştur'}
          </button>
        </footer>
      </div>
    </div>
  );
}
