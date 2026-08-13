# SQLEditör 🗃️⚡

> Kendi DBeaver'ın — Türk Okul Sistemi Veritabanı ile

## Proje Yapısı

```
SQLeditör/
├── docker/
│   ├── docker-compose.yml    ← MySQL container
│   └── mysql/
│       └── init.sql          ← Okul veritabanı + 50 öğrenci verisi
├── backend/                  ← Spring Boot (Java 17)
│   ├── pom.xml
│   ├── mvnw.cmd              ← Maven wrapper (Maven kurmana gerek yok!)
│   └── src/main/java/com/sqleditor/
│       ├── SqlEditorApplication.java
│       ├── controller/ConnectionController.java
│       ├── service/ConnectionService.java
│       └── model/
│           ├── ConnectionRequest.java
│           └── ConnectionResponse.java
└── frontend/                 ← React + Vite
    └── src/
        ├── api/connectionApi.js
        ├── components/ConnectionPanel.jsx
        ├── App.jsx
        └── index.css
```

---

## 🚀 Nasıl Çalıştırılır?

### 1️⃣ MySQL'i Docker ile Başlat

```bash
cd SQLeditör/docker
docker compose up -d
```

Kontrol et: `docker ps` → `sqleditor-mysql` çalışıyor olmalı

---

### 2️⃣ Spring Boot Backend'i Başlat

```bash
cd SQLeditör/backend
set JWT_SECRET=buraya-en-az-32-karakter-rastgele-bir-deger
set CREDENTIAL_ENCRYPTION_KEY=32-byte-base64-aes-anahtari
.\mvnw.cmd spring-boot:run
```

PowerShell'de AES anahtarı üretmek için:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

İlk Docker kurulumundan önce `docker compose up -d` çalıştırılırsa uygulama veritabanı (`sqleditor_app`) otomatik oluşur. Mevcut MySQL volume'ü olan kurulumda bu veritabanını bir kez oluşturmak için `docker/mysql/init.sql` içindeki ilk dört SQL satırını MySQL üzerinde çalıştırın; mevcut `okul_db` veriniz silinmez.

Test: http://localhost:8080/api/connection/health

---

### 3️⃣ React Frontend'i Başlat

```bash
cd SQLeditör/frontend
npm run dev
```

Aç: http://localhost:5173

---

## 📡 API Endpoints

| Method | URL | Açıklama |
|--------|-----|---------|
| GET | `/api/connection/health` | Backend durumu |
| POST | `/api/connection/test` | Bağlantı testi |
| POST | `/api/connection/connect` | Bağlantı kur |
| POST | `/api/auth/register` | Kullanıcı kaydı |
| POST | `/api/auth/login` | Giriş |
| POST | `/api/auth/logout` | Çıkış |
| GET | `/api/connections` | Giriş yapan kullanıcının bağlantı geçmişi |

Tüm bağlantı, şema ve sorgu uçları `Authorization: Bearer <access-token>` ister. Bağlantı tokenı yalnızca `X-Connection-Token` başlığında gönderilir.

### Örnek Request
```json
POST /api/connection/test
{
  "host": "localhost",
  "port": 3306,
  "database": "okul_db",
  "username": "sqleditor",
  "password": "sqleditor123",
  "dbType": "MYSQL"
}
```

---

## 🗄️ Veritabanı Tabloları

| Tablo | Açıklama |
|-------|---------|
| `okullar` | 10 Türk okulu (devlet, özel, vakıf) |
| `bolumler` | 13 bölüm |
| `ogretmenler` | 10 öğretmen |
| `ogrenciler` | 50 öğrenci (ad, soyad, no, bölüm, ortalama...) |
| `dersler` | 15 ders |
| `ogrenci_dersler` | Öğrenci-ders kayıtları |
| `v_ogrenci_ozet` | Özet view |

---

## 🗓️ Aşamalar

- [x] **Aşama 1** — Database Connection ← ŞU AN
- [ ] **Aşama 2** — Schema Explorer (sol panel, tablo ağacı)
- [ ] **Aşama 3** — SQL Editor (CodeMirror)
- [ ] **Aşama 4** — Query Results (tablo görünümü)
- [ ] **Aşama 5** — PostgreSQL, MSSQL desteği

---

## ⚠️ Sorun Giderme

**MySQL bağlantı hatası:**
```bash
docker logs sqleditor-mysql
docker compose down -v && docker compose up -d  # Sıfırdan başlat
```

**Backend başlamıyor:**
- Java 17+ kurulu olduğundan emin ol: `java -version`
- Port 8080 başka program tarafından kullanılıyor olabilir

**Frontend CORS hatası:**
- `vite.config.js` proxy yapılandırması devreye alıyor, sorun olmamalı
