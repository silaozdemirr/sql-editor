# SQLEditor 🚀

**Modern, Web Tabanlı, Yapay Zeka Destekli Veritabanı Yönetim Aracı**

SQLEditor, veritabanlarınıza web tarayıcınız üzerinden güvenle bağlanmanızı, verilerinizi yönetmenizi ve analiz etmenizi sağlayan gelişmiş bir platformdur. Geleneksel masaüstü araçlarının (örn: DBeaver, DataGrip) gücünü modern web teknolojileri, hız ve yapay zeka ile birleştirir.

## 🌟 Öne Çıkan Özellikler

- **Gelişmiş SQL Editörü:** Çoklu sekme desteği, sözdizimi vurgulama (syntax highlighting), otomatik kod formatlama ve klavye kısayolları.
- **Canlı Veri Akışı (Streaming Results):** Devasa (milyonlarca satırlık) sorgu sonuçlarını sunucu belleğini şişirmeden veya tarayıcıyı dondurmadan (Chunk-based NDJSON akışı ile) anında ekrana basma.
- **Yapay Zeka SQL Asistanı:** Sadece ne yapmak istediğinizi söyleyin (Örn: *"En yüksek maaş alan 5 personeli getir"*), gerisini AI halletsin.
- **Dinamik Veri Maskeleme (Data Masking):** Rol tabanlı sıkı güvenlik (Admin, Editor, Read-Only). TC Kimlik, Kredi Kartı, IBAN ve iletişim bilgilerini yetkisiz rollerden anında maskeleme.
- **Devasa Test Verisi Üretimi (Mock Data):** Tablo yapınızı otomatik analiz edip saniyeler içinde milyonlarca satır sahte veriyi, çoklu çekirdek (Multi-threading) mimarisiyle veritabanına enjekte etme.
- **Anında Veri Görselleştirme:** SQL sonuçlarınızı tek tıkla interaktif Pasta (Pie) veya Sütun (Bar) grafiklerine dönüştürün.
- **Görsel ERD (Entity-Relationship) Şeması:** Veritabanınızın tabloları ve aralarındaki yabancı anahtar (Foreign Key) ilişkilerini otomatik olarak şemalaştırın.
- **İçe ve Dışa Aktarım:** Sınırsız Excel/CSV veri dışa aktarımı ve dosya yükleyerek mevcut tablolara hızlı veri aktarımı (Import/Export).
- **İşlem Yönetimi (Transactions):** Auto-commit modunu kapatarak güvenle Commit ve Rollback yapabilme, satır içi veri ekleme ve silme.
- **Kayıtlı Sorgular ve Geçmiş:** Her veritabanı için kendi betiklerinizi saklayın, çalıştırma geçmişinize detaylıca göz atın.

## 🏗️ Teknoloji Yığını

- **Frontend:** React.js, Vite, AG Grid (Yüksek performanslı veri tabloları), Recharts, CodeMirror, Mermaid.js
- **Backend:** Java 21, Spring Boot 3, Spring Security (JWT Auth), HikariCP (Connection Pooling), JDBC Streaming
- **Altyapı:** MySQL, Docker

## 🚀 Hızlı Kurulum ve Çalıştırma

Projeyi lokalinizde saniyeler içinde ayağa kaldırabilirsiniz.

### 1. Docker ile Uygulama Veritabanını Başlatın
Uygulamanın kendi kayıtlarını (kullanıcılar, kayıtlı şifreler, maskeleme kuralları) tutacağı ana veritabanını başlatın:
`ash
cd docker
docker compose up -d
`

### 2. Spring Boot Backend'i Çalıştırın
`ash
cd backend
./mvnw.cmd spring-boot:run
`
*(Mac/Linux için ./mvnw spring-boot:run kullanınız)*

### 3. React Frontend'i Çalıştırın
`ash
cd frontend
npm install
npm run dev
`

🎉 **Hepsi bu kadar!** Tarayıcınızda [http://localhost:5173](http://localhost:5173) adresine giderek uygulamaya giriş yapabilirsiniz.

## 🔒 Güvenlik Notu
Uygulama, bağlandığınız dış veritabanlarının parolalarını AES-256 algoritması ile şifreleyerek kendi veritabanında (sqleditor_app) saklar. Token tabanlı (JWT) mimarisi ile her sorgu oturumu sıkı güvenlik denetimlerinden geçer.
