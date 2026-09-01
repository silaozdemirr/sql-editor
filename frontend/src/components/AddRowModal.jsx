import React, { useState } from 'react';
import { FiX, FiSave } from 'react-icons/fi';
import { executeQuery } from '../api/queryApi';

export default function AddRowModal({ tableName, columns, connectionToken, onClose, onAdded }) {
  const [formData, setFormData] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleChange = (col, value) => {
    setFormData(prev => ({ ...prev, [col]: value }));
  };

  const handleSave = async () => {
    const cols = [];
    const vals = [];
    Object.keys(formData).forEach(col => {
      if (formData[col] !== undefined && formData[col] !== '') {
        cols.push(`\`${col}\``);
        vals.push(`'${String(formData[col]).replace(/'/g, "''")}'`);
      }
    });

    if (cols.length === 0) {
      alert('Lütfen en az bir alanı doldurun.');
      return;
    }

    setIsSubmitting(true);
    try {
      // Split schema and table if present to wrap them in backticks separately
      const tableParts = tableName.split('.');
      const formattedTableName = tableParts.map(part => `\`${part}\``).join('.');
      
      const sql = `INSERT INTO ${formattedTableName} (${cols.join(', ')}) VALUES (${vals.join(', ')})`;
      const res = await executeQuery(connectionToken, sql);
      if (res && res.message && res.message.startsWith('Sorgu hatas')) {
        throw new Error(res.message);
      }
      window.__triggerSchemaRefresh?.();
      onAdded();
    } catch (err) {
      alert('Hata: ' + (err.response?.data?.message || err.message));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ width: '400px' }}>
        <div className="modal-header">
          <h3>{tableName} - Yeni Kayıt Ekle</h3>
          <button className="close-btn" onClick={onClose}><FiX size={20} /></button>
        </div>
        <div className="modal-body" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
          <p style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '15px' }}>
            Otomatik artan (Auto-Increment) ID kolonlarını boş bırakabilirsiniz.
          </p>
          {columns.map(col => (
            <div key={col} style={{ marginBottom: '10px' }}>
              <label style={{ display: 'block', fontSize: '13px', marginBottom: '4px', fontWeight: 500 }}>{col}</label>
              <input
                type="text"
                className="query-input"
                style={{ width: '100%', padding: '8px' }}
                value={formData[col] || ''}
                onChange={(e) => handleChange(col, e.target.value)}
                placeholder={`${col} değeri...`}
              />
            </div>
          ))}
        </div>
        <div className="modal-footer" style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
          <button className="btn-secondary" onClick={onClose} disabled={isSubmitting}>İptal</button>
          <button className="btn-primary" onClick={handleSave} disabled={isSubmitting}>
            <FiSave /> {isSubmitting ? 'Kaydediliyor...' : 'Kaydet'}
          </button>
        </div>
      </div>
    </div>
  );
}
