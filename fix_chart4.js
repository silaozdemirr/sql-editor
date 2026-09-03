const fs = require('fs');

let code = fs.readFileSync('frontend/src/components/QueryResults.jsx', 'utf8');

const regex = /<div style=\{\{ padding: '20px', height: '100%', display: 'flex', flexDirection: 'column' \}\}>\s*<div style=\{\{ display: 'flex', gap: '15px', marginBottom: '20px' \}\}>/m;

const replacement = `<div style={{ padding: '20px', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ marginBottom: '15px', padding: '12px 16px', background: 'rgba(59, 130, 246, 0.1)', borderLeft: '4px solid #3b82f6', borderRadius: '4px', color: 'var(--text-secondary)', fontSize: '13px', display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
        <FiInfo size={18} color="#3b82f6" style={{ flexShrink: 0, marginTop: '2px' }} />
        <div>
          <strong style={{ color: 'var(--text-primary)', display: 'block', marginBottom: '6px', fontSize: '14px' }}>Grafik Aracı Nasıl Kullanılır?</strong>
          Anlamlı bir grafik çizebilmek için SQL sorgunuzun sonucunda <b>kategorik</b> ve <b>sayısal</b> veriler olmalıdır (Genellikle <code>GROUP BY</code> ve <code>COUNT, SUM</code> vb. kullanılarak elde edilir).
          <ul style={{ margin: '8px 0 0 0', paddingLeft: '20px', lineHeight: '1.5' }}>
            <li><b>Kategori (X Ekseni):</b> Grafiğin alt kısmında veya pasta dilimlerinde isim olarak görünecek metinsel kolon. <i>(Örn: bolum_adi, sehir)</i></li>
            <li><b>Değer (Y Ekseni):</b> Grafiğin çubuk boyunu veya dilim büyüklüğünü belirleyecek <b>sayısal</b> kolon. <i>(Örn: toplam_satis, musteri_sayisi)</i></li>
          </ul>
        </div>
      </div>
      <div style={{ display: 'flex', gap: '15px', marginBottom: '20px', alignItems: 'center', flexWrap: 'wrap' }}>`;

code = code.replace(regex, replacement);

fs.writeFileSync('frontend/src/components/QueryResults.jsx', code, 'utf8');
console.log("SUCCESS");
