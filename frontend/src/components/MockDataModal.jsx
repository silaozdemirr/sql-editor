import React, { useState, useEffect } from 'react';
import { FiX } from 'react-icons/fi';
import { generateMockData, getTableColumns } from '../api/schemaApi';

const FAKER_TYPES = [
  { value: 'none', label: '-- Sabit/Atla --' },
  { value: 'Name.fullName', label: 'Tam İsim' },
  { value: 'Name.firstName', label: 'İlk İsim' },
  { value: 'Name.lastName', label: 'Soyisim' },
  { value: 'Internet.email', label: 'E-Posta' },
  { value: 'PhoneNumber.cellPhone', label: 'Telefon' },
  { value: 'Address.fullAddress', label: 'Tam Adres' },
  { value: 'Address.city', label: 'Şehir' },
  { value: 'Address.country', label: 'Ülke' },
  { value: 'Company.name', label: 'Şirket Adı' },
  { value: 'Number.randomInt', label: 'Rastgele Sayı (1-10000)' },
  { value: 'Number.randomDouble', label: 'Ondalıklı Sayı' },
  { value: 'Date.birthday', label: 'Tarih (Doğum Günü)' },
  { value: 'Date.past', label: 'Tarih (Geçmiş)' },
  { value: 'Date.future', label: 'Tarih (Gelecek)' },
  { value: 'Lorem.word', label: 'Rastgele Kelime' },
  { value: 'Lorem.sentence', label: 'Rastgele Cümle' },
  { value: 'Color.name', label: 'Renk' }
];

export default function MockDataModal({ database, tableName, connectionToken, onClose, onGenerated }) {
  const [columns, setColumns] = useState([]);
  const [mappings, setMappings] = useState({});
  const [rowCount, setRowCount] = useState(100);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [loadingCols, setLoadingCols] = useState(true);

  useEffect(() => {
    const loadCols = async () => {
      try {
        const cols = await getTableColumns(connectionToken, database, tableName);
        setColumns(cols);
        
        // Auto-guess faker types
        const initialMap = {};
        cols.forEach(c => {
          let type = 'none';
          const lower = c.columnName.toLowerCase();
          if (c.autoIncrement) type = 'none';
          else if (lower.includes('mail')) type = 'Internet.email';
          else if (lower.includes('ad') || lower.includes('name')) type = 'Name.firstName';
          else if (lower.includes('soyad') || lower.includes('lastname')) type = 'Name.lastName';
          else if (lower.includes('tel') || lower.includes('phone')) type = 'PhoneNumber.cellPhone';
          else if (lower.includes('adres') || lower.includes('address')) type = 'Address.fullAddress';
          else if (lower.includes('sehir') || lower.includes('city')) type = 'Address.city';
          else if (lower.includes('fiyat') || lower.includes('price')) type = 'Number.randomDouble';
          else if (lower.includes('tarih') || lower.includes('date')) type = 'Date.past';
          else if (c.dataType.includes('INT')) type = 'Number.randomInt';
          else if (c.dataType.includes('VARCHAR')) type = 'Lorem.word';
          initialMap[c.columnName] = type;
        });
        setMappings(initialMap);
      } catch (err) {
        setError('Kolonlar yüklenemedi: ' + err.message);
      } finally {
        setLoadingCols(false);
      }
    };
    loadCols();
  }, [database, tableName, connectionToken]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (rowCount < 1 || rowCount > 100000) {
      setError('Satır sayısı 1 ile 100,000 arasında olmalıdır.');
      return;
    }

    const payloadMappings = Object.entries(mappings)
      .filter(([col, type]) => type !== 'none')
      .map(([col, type]) => ({ columnName: col, fakerType: type }));

    if (payloadMappings.length === 0) {
      setError('En az bir kolon için veri üretici seçilmelidir.');
      return;
    }

    try {
      setIsSubmitting(true);
      setError('');
      await generateMockData({
        databaseName: database,
        tableName: tableName,
        rowCount: parseInt(rowCount, 10),
        mappings: payloadMappings
      });
      onGenerated();
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.6)', zIndex: 99999, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ background: 'var(--bg-layer-1)', width: '90vw', maxWidth: '600px', borderRadius: '8px', display: 'flex', flexDirection: 'column', maxHeight: '90vh', boxShadow: '0 10px 25px rgba(0,0,0,0.5)' }}>
        <header style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>Sentetik Veri Üret - {tableName}</h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-primary)', cursor: 'pointer' }}><FiX size={24} /></button>
        </header>
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1 }}>
          {error && <div className="schema-error" style={{ marginBottom: '16px' }}>{error}</div>}
          
          <div className="form-group">
            <label className="form-label">Üretilecek Satır Sayısı</label>
            <input className="form-input" type="number" min="1" max="100000" value={rowCount} onChange={e => setRowCount(e.target.value)} />
          </div>

          <div style={{ marginTop: '24px' }}>
            <h3 style={{ fontSize: '16px', margin: '0 0 12px 0' }}>Kolon Eşleşmeleri</h3>
            {loadingCols ? <p>Yükleniyor...</p> : (
              <table className="admin-table" style={{ width: '100%' }}>
                <thead>
                  <tr>
                    <th>Kolon Adı</th>
                    <th>Veri Tipi</th>
                    <th>Üretici (Faker)</th>
                  </tr>
                </thead>
                <tbody>
                  {columns.map(c => (
                    <tr key={c.columnName}>
                      <td>{c.columnName} {c.isPrimaryKey && '🔑'}</td>
                      <td>{c.dataType}</td>
                      <td>
                        <select 
                          className="form-input" 
                          style={{ padding: '4px', fontSize: '13px' }}
                          value={mappings[c.columnName] || 'none'}
                          onChange={e => setMappings({...mappings, [c.columnName]: e.target.value})}
                          disabled={c.autoIncrement}
                        >
                          {c.autoIncrement ? <option value="none">Oto-Artan (Atla)</option> : FAKER_TYPES.map(ft => (
                            <option key={ft.value} value={ft.value}>{ft.label}</option>
                          ))}
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
        <footer style={{ padding: '16px 20px', borderTop: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          <button className="btn-secondary" onClick={onClose} disabled={isSubmitting}>İptal</button>
          <button className="btn-primary" onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? 'Üretiliyor...' : 'Veri Üret'}
          </button>
        </footer>
      </div>
    </div>
  );
}
