import React, { useState, useEffect } from 'react';
import { FiX } from 'react-icons/fi';
import { generateMockData, getTableColumns } from '../api/schemaApi';

const FAKER_TYPES = [
  { value: 'none', label: '-- Sabit/Atla --' },
  { value: 'Name.fullName', label: 'Tam İsim' },
  { value: 'Name.firstName', label: 'İlk İsim' },
  { value: 'Name.lastName', label: 'Soyisim' },
  { value: 'Gender.types', label: 'Cinsiyet' },
  { value: 'Internet.email', label: 'E-Posta' },
  { value: 'Internet.password', label: 'Şifre' },
  { value: 'PhoneNumber.cellPhone', label: 'Telefon' },
  { value: 'Address.fullAddress', label: 'Tam Adres' },
  { value: 'Address.city', label: 'Şehir' },
  { value: 'Address.country', label: 'Ülke' },
  { value: 'Address.zipCode', label: 'Posta Kodu' },
  { value: 'Company.name', label: 'Şirket Adı' },
  { value: 'Company.industry', label: 'Sektör' },
  { value: 'Job.title', label: 'Meslek / Ünvan' },
  { value: 'Commerce.productName', label: 'Ürün Adı' },
  { value: 'Commerce.price', label: 'Fiyat' },
  { value: 'Commerce.department', label: 'Kategori / Departman' },
  { value: 'Finance.creditCard', label: 'Kredi Kartı' },
  { value: 'Number.age', label: 'Yaş (0-100)' },
  { value: 'Number.randomInt', label: 'Rastgele Sayı (1-100000)' },
  { value: 'Number.randomDouble', label: 'Ondalıklı Sayı' },
  { value: 'Date.now', label: 'Tarih (Şu An)' },
  { value: 'Date.birthday', label: 'Tarih (Doğum Günü)' },
  { value: 'Date.past', label: 'Tarih (Geçmiş)' },
  { value: 'Date.future', label: 'Tarih (Gelecek)' },
  { value: 'Lorem.word', label: 'Rastgele Kelime' },
  { value: 'Lorem.sentence', label: 'Rastgele Cümle' },
  { value: 'Color.name', label: 'Renk' },
  { value: 'Bool.random', label: 'Evet/Hayır (Boolean)' },
  { value: 'Internet.ipv4Address', label: 'IP Adresi (IPv4)' },
  { value: 'Internet.macAddress', label: 'MAC Adresi' }
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
        const cols = await getTableColumns(connectionToken, tableName, database);
        setColumns(cols);
        
        // Auto-guess faker types
        const initialMap = {};
        cols.forEach(c => {
          let type = 'none';
          const lower = c.name.toLowerCase();
          if ((c.extra && c.extra.toLowerCase().includes("auto_increment"))) type = 'none';
          else if (lower.includes('mail')) type = 'Internet.email';
          else if (lower === 'ad' || lower.includes('first_name') || lower.includes('firstname')) type = 'Name.firstName';
          else if (lower === 'soyad' || lower.includes('last_name') || lower.includes('lastname')) type = 'Name.lastName';
          else if (lower.includes('ad') || lower.includes('name')) type = 'Name.fullName';
          else if (lower.includes('tel') || lower.includes('phone')) type = 'PhoneNumber.cellPhone';
          else if (lower.includes('yas') || lower.includes('age') || lower.includes('yaş')) type = 'Number.age';
          else if (lower.includes('adres') || lower.includes('address')) type = 'Address.fullAddress';
          else if (lower.includes('sehir') || lower.includes('city') || lower.includes('şehir')) type = 'Address.city';
          else if (lower.includes('ulke') || lower.includes('country') || lower.includes('ülke')) type = 'Address.country';
          else if (lower.includes('sifre') || lower.includes('password') || lower.includes('şifre')) type = 'Internet.password';
          else if (lower.includes('fiyat') || lower.includes('price')) type = 'Commerce.price';
          else if (lower.includes('urun') || lower.includes('product') || lower.includes('ürün')) type = 'Commerce.productName';
          else if (lower.includes('meslek') || lower.includes('job') || lower.includes('title')) type = 'Job.title';
          else if (lower.includes('tarih') || lower.includes('date') || lower.includes('time') || lower.includes('created')) type = 'Date.past';
          else if (lower.includes('dogum') || lower.includes('birth')) type = 'Date.birthday';
          else if (lower.includes('cinsiyet') || lower.includes('gender')) type = 'Gender.types';
          else if (c.dataType.includes('BOOL') || c.dataType.includes('TINYINT(1)') || lower.includes('is_') || lower.includes('aktif') || lower.includes('active')) type = 'Bool.random';
          else if (c.dataType.includes('INT')) type = 'Number.randomInt';
          else if (c.dataType.includes('VARCHAR')) type = 'Lorem.word';
          initialMap[c.name] = type;
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
    if (rowCount < 1 || false /* sinirsiz */) {
      setError('Satır sayısı en az 1 olmalıdır.');
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
      alert(rowCount + " satır sentetik veri başarıyla tabloya eklendi!\n(Verileri görmek için tablonun üzerine tıklayabilirsiniz)");
      onGenerated();
    } catch (err) {
      setError(err.response?.data?.message || err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.7)', zIndex: 99999, display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(3px)' }}>
      <div style={{ background: 'var(--bg-card, #242424)', width: '90vw', maxWidth: '600px', borderRadius: '8px', display: 'flex', flexDirection: 'column', maxHeight: '90vh', boxShadow: '0 10px 25px rgba(0,0,0,0.8)', border: '1px solid var(--border-muted)' }}>
        <header style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-subtle)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>Sentetik Veri Üret - {tableName}</h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-primary)', cursor: 'pointer' }}><FiX size={24} /></button>
        </header>
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1 }}>
          {error && <div className="schema-error" style={{ marginBottom: '16px' }}>{error}</div>}
          
          <div className="form-group">
            <label className="form-label">Üretilecek Satır Sayısı</label>
            <input className="form-input" type="number" min="1"  value={rowCount} onChange={e => setRowCount(e.target.value)} />
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
                    <tr key={c.name}>
                      <td>{c.name} {c.primaryKey && '🔑'}</td>
                      <td>{c.dataType}</td>
                      <td>
                        <select 
                          className="form-input" 
                          style={{ padding: '4px', fontSize: '13px' }}
                          value={mappings[c.name] || 'none'}
                          onChange={e => setMappings({...mappings, [c.name]: e.target.value})}
                          disabled={(c.extra && c.extra.toLowerCase().includes("auto_increment"))}
                        >
                          {(c.extra && c.extra.toLowerCase().includes("auto_increment")) ? <option value="none">Oto-Artan (Atla)</option> : FAKER_TYPES.map(ft => (
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
          <button className="btn btn-secondary" onClick={onClose} disabled={isSubmitting}>İptal</button>
          <button className="btn btn-primary" onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? 'Üretiliyor...' : 'Veri Üret'}
          </button>
        </footer>
      </div>
    </div>
  );
}
