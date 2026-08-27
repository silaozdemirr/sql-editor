import React, { useState } from 'react';
import { FiPlus, FiX, FiCheck, FiTrash2 } from 'react-icons/fi';
import { createTable } from '../api/schemaApi';

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
      setError('Tablo adi girilmelidir.');
      return;
    }
    if (columns.length === 0) {
      setError('En az bir kolon eklenmelidir.');
      return;
    }
    for (let c of columns) {
      if (!c.name.trim()) {
        setError('Tüm kolonlarin adi olmalidir.');
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
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.6)', zIndex: 99999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ background: 'var(--bg-layer-1)', width: '90vw', maxWidth: '800px', borderRadius: '8px', display: 'flex', flexDirection: 'column', maxHeight: '90vh', boxShadow: '0 10px 25px rgba(0,0,0,0.5)' }}>
        <header style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>Yeni Tablo Olustur - {database}</h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-primary)', cursor: 'pointer' }}><FiX size={24} /></button>
        </header>
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1 }}>
          {error && <div className="schema-error" style={{ marginBottom: '16px' }}>{error}</div>}
          
          <div className="form-group">
            <label className="form-label">Tablo Adi</label>
            <input className="form-input" value={tableName} onChange={e => setTableName(e.target.value)} placeholder="ornek_tablo" />
          </div>

          <div style={{ marginTop: '24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <h3 style={{ fontSize: '16px', margin: 0 }}>Kolonlar</h3>
              <button className="btn-secondary btn-sm" onClick={addColumn}><FiPlus /> Kolon Ekle</button>
            </div>
            
            <table className="admin-table" style={{ width: '100%' }}>
              <thead>
                <tr>
                  <th>Ad</th>
                  <th>Tip</th>
                  <th title="Primary Key">PK</th>
                  <th title="Not Null">NN</th>
                  <th title="Auto Increment">AI</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {columns.map(c => (
                  <tr key={c.id}>
                    <td>
                      <input className="form-input" style={{ padding: '4px 8px', fontSize: '13px' }} value={c.name} onChange={e => updateColumn(c.id, 'name', e.target.value)} placeholder="kolon_adi" />
                    </td>
                    <td>
                      <input className="form-input" style={{ padding: '4px 8px', fontSize: '13px' }} value={c.type} onChange={e => updateColumn(c.id, 'type', e.target.value)} placeholder="VARCHAR(255)" />
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <input type="checkbox" checked={c.primaryKey} onChange={e => updateColumn(c.id, 'primaryKey', e.target.checked)} />
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <input type="checkbox" checked={c.notNull} onChange={e => updateColumn(c.id, 'notNull', e.target.checked)} />
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <input type="checkbox" checked={c.autoIncrement} onChange={e => updateColumn(c.id, 'autoIncrement', e.target.checked)} />
                    </td>
                    <td>
                      <button style={{ color: 'var(--text-danger)', background: 'transparent', border: 'none', cursor: 'pointer' }} onClick={() => removeColumn(c.id)}>
                        <FiTrash2 />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <footer style={{ padding: '16px 20px', borderTop: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          <button className="btn-secondary" onClick={onClose} disabled={isSubmitting}>İptal</button>
          <button className="btn-primary" onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? 'Olusturuluyor...' : 'Olustur'}
          </button>
        </footer>
      </div>
    </div>
  );
}
