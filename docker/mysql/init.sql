-- ============================================================
-- SQL Editör Projesi - Türk Okul Sistemi Örnek Verisi
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Veritabanı oluştur
CREATE DATABASE IF NOT EXISTS okul_db CHARACTER SET utf8mb4 COLLATE utf8mb4_turkish_ci;
USE okul_db;

-- ============================================================
-- TABLO: okullar
-- ============================================================
CREATE TABLE IF NOT EXISTS okullar (
    okul_id      INT AUTO_INCREMENT PRIMARY KEY,
    okul_adi     VARCHAR(150) NOT NULL,
    sehir        VARCHAR(100) NOT NULL,
    ilce         VARCHAR(100),
    tur          ENUM('devlet','ozel','vakif') NOT NULL DEFAULT 'devlet',
    kurulis_yili INT,
    adres        TEXT,
    telefon      VARCHAR(20),
    email        VARCHAR(150),
    ogrenci_sayisi INT DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLO: bolumler
-- ============================================================
CREATE TABLE IF NOT EXISTS bolumler (
    bolum_id    INT AUTO_INCREMENT PRIMARY KEY,
    bolum_adi   VARCHAR(150) NOT NULL,
    bolum_kodu  VARCHAR(20) UNIQUE NOT NULL,
    okul_id     INT NOT NULL,
    kontenjan   INT DEFAULT 30,
    sure_yil    INT DEFAULT 4,
    aktif       BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (okul_id) REFERENCES okullar(okul_id) ON DELETE CASCADE
);

-- ============================================================
-- TABLO: ogretmenler
-- ============================================================
CREATE TABLE IF NOT EXISTS ogretmenler (
    ogretmen_id   INT AUTO_INCREMENT PRIMARY KEY,
    tc_kimlik     VARCHAR(11) UNIQUE NOT NULL,
    ad            VARCHAR(100) NOT NULL,
    soyad         VARCHAR(100) NOT NULL,
    brans         VARCHAR(100),
    email         VARCHAR(150),
    telefon       VARCHAR(20),
    okul_id       INT,
    ise_baslama   DATE,
    aktif         BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (okul_id) REFERENCES okullar(okul_id)
);

-- ============================================================
-- TABLO: ogrenciler
-- ============================================================
CREATE TABLE IF NOT EXISTS ogrenciler (
    ogrenci_id     INT AUTO_INCREMENT PRIMARY KEY,
    ogrenci_no     VARCHAR(20) UNIQUE NOT NULL,
    tc_kimlik      VARCHAR(11) UNIQUE,
    ad             VARCHAR(100) NOT NULL,
    soyad          VARCHAR(100) NOT NULL,
    dogum_tarihi   DATE,
    cinsiyet       ENUM('E','K') NOT NULL,
    bolum_id       INT NOT NULL,
    sinif          TINYINT NOT NULL DEFAULT 1,
    ortalama       DECIMAL(4,2) DEFAULT 0.00,
    kayit_tarihi   DATE NOT NULL,
    adres          TEXT,
    telefon        VARCHAR(20),
    email          VARCHAR(150),
    aktif          BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (bolum_id) REFERENCES bolumler(bolum_id)
);

-- ============================================================
-- TABLO: dersler
-- ============================================================
CREATE TABLE IF NOT EXISTS dersler (
    ders_id       INT AUTO_INCREMENT PRIMARY KEY,
    ders_kodu     VARCHAR(20) UNIQUE NOT NULL,
    ders_adi      VARCHAR(150) NOT NULL,
    bolum_id      INT NOT NULL,
    ogretmen_id   INT,
    kredi         TINYINT DEFAULT 3,
    sinif_seviyesi TINYINT DEFAULT 1,
    zorunlu       BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (bolum_id) REFERENCES bolumler(bolum_id),
    FOREIGN KEY (ogretmen_id) REFERENCES ogretmenler(ogretmen_id)
);

-- ============================================================
-- TABLO: ogrenci_dersler (kayıt tablosu - many-to-many)
-- ============================================================
CREATE TABLE IF NOT EXISTS ogrenci_dersler (
    kayit_id     INT AUTO_INCREMENT PRIMARY KEY,
    ogrenci_id   INT NOT NULL,
    ders_id      INT NOT NULL,
    donem        VARCHAR(20) NOT NULL COMMENT 'örn: 2024-Güz',
    vize_notu    DECIMAL(5,2),
    final_notu   DECIMAL(5,2),
    harf_notu    CHAR(2),
    gecti        BOOLEAN,
    UNIQUE KEY uniq_kayit (ogrenci_id, ders_id, donem),
    FOREIGN KEY (ogrenci_id) REFERENCES ogrenciler(ogrenci_id),
    FOREIGN KEY (ders_id) REFERENCES dersler(ders_id)
);

-- ============================================================
-- VERİ: okullar
-- ============================================================
INSERT INTO okullar (okul_adi, sehir, ilce, tur, kurulis_yili, telefon, email, ogrenci_sayisi) VALUES
('Ankara Fen Lisesi', 'Ankara', 'Çankaya', 'devlet', 1956, '0312-4451122', 'info@ankarafenlisesi.gov.tr', 1200),
('İstanbul Teknik Üniversitesi Vakıf Lisesi', 'İstanbul', 'Maslak', 'vakif', 1998, '0212-3336677', 'info@ituvakif.edu.tr', 800),
('Kadıköy Anadolu Lisesi', 'İstanbul', 'Kadıköy', 'devlet', 1963, '0216-3382211', 'info@kadikoyandolu.meb.gov.tr', 1500),
('Boğaziçi Üniversitesi Ek Kampüsü Lisesi', 'İstanbul', 'Beşiktaş', 'devlet', 1971, '0212-3594545', 'lise@boun.edu.tr', 600),
('Hacettepe Üniversitesi Lisesi', 'Ankara', 'Sıhhiye', 'devlet', 1967, '0312-3051010', 'lise@hacettepe.edu.tr', 900),
('İzmir Atatürk Lisesi', 'İzmir', 'Konak', 'devlet', 1905, '0232-4833344', 'info@izmiratatürk.meb.gov.tr', 2000),
('Bursa Uludağ Fen Lisesi', 'Bursa', 'Osmangazi', 'devlet', 1994, '0224-2719090', 'info@uludagfen.meb.gov.tr', 700),
('TED Ankara Koleji', 'Ankara', 'Kolej', 'ozel', 1949, '0312-4407200', 'info@tedankara.k12.tr', 2500),
('Konya Meram Anadolu Lisesi', 'Konya', 'Meram', 'devlet', 1985, '0332-3234455', 'info@konya.meb.gov.tr', 1100),
('Gaziantep Fen Lisesi', 'Gaziantep', 'Şahinbey', 'devlet', 1996, '0342-3361234', 'info@gaziantepfen.meb.gov.tr', 650);

-- ============================================================
-- VERİ: bolumler
-- ============================================================
INSERT INTO bolumler (bolum_adi, bolum_kodu, okul_id, kontenjan, sure_yil) VALUES
('Sayısal', 'SAY-01', 1, 120, 4),
('Sözel', 'SOZ-01', 1, 80, 4),
('Yabancı Dil', 'YDL-01', 1, 60, 4),
('Mühendislik Hazırlık', 'MUH-02', 2, 100, 4),
('Fen Bilimleri', 'FEN-02', 2, 80, 4),
('Sayısal', 'SAY-03', 3, 150, 4),
('Sözel', 'SOZ-03', 3, 100, 4),
('Yabancı Dil', 'YDL-03', 3, 80, 4),
('Özel Bilim', 'OZL-04', 4, 60, 4),
('Fen Bilimleri', 'FEN-05', 5, 90, 4),
('Bilişim Teknolojileri', 'BIL-08', 8, 120, 4),
('Matematik-Fen', 'MAT-08', 8, 100, 4),
('Türk Dili ve Edebiyatı', 'TDE-08', 8, 80, 4);

-- ============================================================
-- VERİ: ogretmenler
-- ============================================================
INSERT INTO ogretmenler (tc_kimlik, ad, soyad, brans, email, telefon, okul_id, ise_baslama) VALUES
('12345678901', 'Ahmet', 'Yılmaz', 'Matematik', 'ahmet.yilmaz@ankarafenl.gov.tr', '0532-1112233', 1, '2005-09-01'),
('23456789012', 'Fatma', 'Kaya', 'Fizik', 'fatma.kaya@ankarafenl.gov.tr', '0533-2223344', 1, '2008-09-01'),
('34567890123', 'Mehmet', 'Demir', 'Kimya', 'mehmet.demir@ankarafenl.gov.tr', '0534-3334455', 1, '2010-09-01'),
('45678901234', 'Ayşe', 'Çelik', 'Türkçe', 'ayse.celik@kadikoy.gov.tr', '0535-4445566', 3, '2012-09-01'),
('56789012345', 'Mustafa', 'Şahin', 'Tarih', 'mustafa.sahin@kadikoy.gov.tr', '0536-5556677', 3, '2015-09-01'),
('67890123456', 'Zeynep', 'Arslan', 'İngilizce', 'zeynep.arslan@tedankara.k12.tr', '0537-6667788', 8, '2018-09-01'),
('78901234567', 'İbrahim', 'Koç', 'Biyoloji', 'ibrahim.koc@hacettepe.edu.tr', '0538-7778899', 5, '2007-09-01'),
('89012345678', 'Hatice', 'Erdoğan', 'Coğrafya', 'hatice.erdogan@izmir.gov.tr', '0539-8889900', 6, '2011-09-01'),
('90123456789', 'Ömer', 'Aydın', 'Matematik', 'omer.aydin@tedankara.k12.tr', '0541-9990011', 8, '2014-09-01'),
('01234567890', 'Elif', 'Kılıç', 'Bilişim', 'elif.kilic@tedankara.k12.tr', '0542-0001122', 8, '2019-09-01');

-- ============================================================
-- VERİ: ogrenciler (50 öğrenci)
-- ============================================================
INSERT INTO ogrenciler (ogrenci_no, tc_kimlik, ad, soyad, dogum_tarihi, cinsiyet, bolum_id, sinif, ortalama, kayit_tarihi, telefon, email) VALUES
('2024AFL001', '11111111111', 'Emre', 'Arslan', '2008-03-15', 'E', 1, 2, 87.50, '2023-09-04', '0555-1010101', 'emre.arslan@ogrenci.com'),
('2024AFL002', '22222222222', 'Selin', 'Yıldız', '2008-07-22', 'K', 1, 2, 92.30, '2023-09-04', '0555-2020202', 'selin.yildiz@ogrenci.com'),
('2024AFL003', '33333333333', 'Kerem', 'Öztürk', '2007-11-08', 'E', 1, 3, 78.40, '2022-09-05', '0555-3030303', 'kerem.ozturk@ogrenci.com'),
('2024AFL004', '44444444444', 'Büşra', 'Doğan', '2007-05-30', 'K', 1, 3, 95.10, '2022-09-05', '0555-4040404', 'busra.dogan@ogrenci.com'),
('2024AFL005', '55555555555', 'Tarık', 'Yılmaz', '2009-01-12', 'E', 1, 1, 81.20, '2024-09-02', '0555-5050505', 'tarik.yilmaz@ogrenci.com'),
('2024AFL006', '66666666666', 'Deniz', 'Kara', '2008-09-18', 'K', 2, 2, 88.70, '2023-09-04', '0555-6060606', 'deniz.kara@ogrenci.com'),
('2024AFL007', '77777777777', 'Baran', 'Şimşek', '2008-04-25', 'E', 2, 2, 73.60, '2023-09-04', '0555-7070707', 'baran.simsek@ogrenci.com'),
('2024AFL008', '88888888888', 'Nisan', 'Çetin', '2007-12-03', 'K', 2, 3, 91.80, '2022-09-05', '0555-8080808', 'nisan.cetin@ogrenci.com'),
('2024AFL009', '99999999999', 'Arda', 'Polat', '2009-06-14', 'E', 3, 1, 85.40, '2024-09-02', '0555-9090909', 'arda.polat@ogrenci.com'),
('2024AFL010', '10101010101', 'Ceren', 'Avcı', '2008-02-28', 'K', 3, 2, 94.20, '2023-09-04', '0555-1001001', 'ceren.avci@ogrenci.com'),
('2024KAD001', '12121212121', 'Mert', 'Güneş', '2008-08-10', 'E', 6, 2, 82.30, '2023-09-04', '0556-1010101', 'mert.gunes@ogrenci.com'),
('2024KAD002', '23232323232', 'İpek', 'Demirci', '2008-11-22', 'K', 6, 2, 89.50, '2023-09-04', '0556-2020202', 'ipek.demirci@ogrenci.com'),
('2024KAD003', '34343434343', 'Erhan', 'Bulut', '2007-03-17', 'E', 6, 3, 76.80, '2022-09-05', '0556-3030303', 'erhan.bulut@ogrenci.com'),
('2024KAD004', '45454545454', 'Pınar', 'Çakır', '2009-09-05', 'K', 7, 1, 93.60, '2024-09-02', '0556-4040404', 'pinar.cakir@ogrenci.com'),
('2024KAD005', '56565656565', 'Batuhan', 'Koç', '2008-01-30', 'E', 7, 2, 79.40, '2023-09-04', '0556-5050505', 'batuhan.koc@ogrenci.com'),
('2024KAD006', '67676767676', 'Merve', 'Yalçın', '2007-07-15', 'K', 7, 3, 86.90, '2022-09-05', '0556-6060606', 'merve.yalcin@ogrenci.com'),
('2024KAD007', '78787878787', 'Oğulcan', 'Başaran', '2008-05-20', 'E', 8, 2, 91.20, '2023-09-04', '0556-7070707', 'ogulcan.basaran@ogrenci.com'),
('2024KAD008', '89898989898', 'Tuğba', 'Kaplan', '2009-02-08', 'K', 8, 1, 84.70, '2024-09-02', '0556-8080808', 'tugba.kaplan@ogrenci.com'),
('2024TED001', '90909090909', 'Alp', 'Karahan', '2008-06-12', 'E', 11, 2, 96.30, '2023-09-04', '0557-1010101', 'alp.karahan@ogrenci.com'),
('2024TED002', '01010101010', 'Zehra', 'Uçar', '2008-10-25', 'K', 11, 2, 88.40, '2023-09-04', '0557-2020202', 'zehra.ucar@ogrenci.com'),
('2024TED003', '13131313131', 'Furkan', 'Duman', '2007-04-18', 'E', 11, 3, 82.10, '2022-09-05', '0557-3030303', 'furkan.duman@ogrenci.com'),
('2024TED004', '24242424242', 'Melis', 'Tekin', '2009-08-30', 'K', 12, 1, 97.80, '2024-09-02', '0557-4040404', 'melis.tekin@ogrenci.com'),
('2024TED005', '35353535353', 'Doruk', 'Acar', '2008-03-22', 'E', 12, 2, 85.60, '2023-09-04', '0557-5050505', 'doruk.acar@ogrenci.com'),
('2024TED006', '46464646464', 'Aslı', 'Yıldırım', '2007-09-14', 'K', 12, 3, 90.30, '2022-09-05', '0557-6060606', 'asli.yildirim@ogrenci.com'),
('2024TED007', '57575757575', 'Umut', 'Çelik', '2008-12-05', 'E', 13, 2, 78.90, '2023-09-04', '0557-7070707', 'umut.celik@ogrenci.com'),
('2024TED008', '68686868686', 'Dila', 'Korkmaz', '2009-04-28', 'K', 13, 1, 83.50, '2024-09-02', '0557-8080808', 'dila.korkmaz@ogrenci.com'),
('2024TED009', '79797979797', 'Can', 'Özer', '2008-07-11', 'E', 13, 2, 74.20, '2023-09-04', '0557-9090909', 'can.ozer@ogrenci.com'),
('2024TED010', '80808080808', 'Nur', 'Aydın', '2007-01-07', 'K', 13, 3, 88.80, '2022-09-05', '0557-0100100', 'nur.aydin@ogrenci.com'),
('2024HAC001', '91919191919', 'Cem', 'Kılıç', '2008-05-16', 'E', 10, 2, 86.40, '2023-09-04', '0558-1010101', 'cem.kilic@ogrenci.com'),
('2024HAC002', '02020202020', 'Yağmur', 'Şahin', '2009-03-09', 'K', 10, 1, 92.70, '2024-09-02', '0558-2020202', 'yagmur.sahin@ogrenci.com'),
('2024HAC003', '14141414141', 'Sercan', 'Arslan', '2007-08-24', 'E', 10, 3, 77.30, '2022-09-05', '0558-3030303', 'sercan.arslan@ogrenci.com'),
('2024HAC004', '25252525252', 'Naz', 'Erdoğan', '2008-11-15', 'K', 10, 2, 89.10, '2023-09-04', '0558-4040404', 'naz.erdogan@ogrenci.com'),
('2024HAC005', '36363636363', 'Taha', 'Dönmez', '2009-06-02', 'E', 10, 1, 81.60, '2024-09-02', '0558-5050505', 'taha.donmez@ogrenci.com'),
('2024IZM001', '47474747474', 'Defne', 'Çetin', '2008-02-19', 'K', 9, 2, 95.40, '2023-09-04', '0559-1010101', 'defne.cetin@ogrenci.com'),
('2024IZM002', '58585858585', 'Enis', 'Yıldız', '2007-10-31', 'E', 9, 3, 80.20, '2022-09-05', '0559-2020202', 'enis.yildiz@ogrenci.com'),
('2024IZM003', '69696969696', 'Lara', 'Güler', '2009-07-14', 'K', 9, 1, 87.90, '2024-09-02', '0559-3030303', 'lara.guler@ogrenci.com'),
('2024IZM004', '70707070707', 'Kaan', 'Özcan', '2008-04-27', 'E', 9, 2, 73.80, '2023-09-04', '0559-4040404', 'kaan.ozcan@ogrenci.com'),
('2024BOG001', '81818181818', 'Rüya', 'Aktaş', '2008-09-20', 'K', 9, 2, 91.50, '2023-09-04', '0560-1010101', 'ruya.aktas@ogrenci.com'),
('2024BOG002', '92929292929', 'Selim', 'Yalçın', '2007-06-08', 'E', 9, 3, 84.30, '2022-09-05', '0560-2020202', 'selim.yalcin@ogrenci.com'),
('2024BOG003', '03030303030', 'Eylül', 'Karaca', '2009-01-25', 'K', 9, 1, 78.60, '2024-09-02', '0560-3030303', 'eylul.karaca@ogrenci.com'),
('2024ULU001', '15151515151', 'Efe', 'Demiral', '2008-08-13', 'E', 4, 2, 86.70, '2023-09-04', '0561-1010101', 'efe.demiral@ogrenci.com'),
('2024ULU002', '26262626262', 'Sude', 'Özdemir', '2009-05-06', 'K', 4, 1, 90.40, '2024-09-02', '0561-2020202', 'sude.ozdemir@ogrenci.com'),
('2024ULU003', '37373737373', 'İlker', 'Kaya', '2007-12-19', 'E', 4, 3, 75.20, '2022-09-05', '0561-3030303', 'ilker.kaya@ogrenci.com'),
('2024GAZ001', '48484848484', 'Ecrin', 'Aydın', '2008-03-04', 'K', 5, 2, 93.80, '2023-09-04', '0562-1010101', 'ecrin.aydin@ogrenci.com'),
('2024GAZ002', '59595959595', 'Yusuf', 'Şimşek', '2009-10-17', 'E', 5, 1, 82.50, '2024-09-02', '0562-2020202', 'yusuf.simsek@ogrenci.com'),
('2024GAZ003', '60606060606', 'İrem', 'Bulut', '2007-07-30', 'K', 5, 3, 88.10, '2022-09-05', '0562-3030303', 'irem.bulut@ogrenci.com'),
('2024KON001', '71717171717', 'Berkay', 'Doğan', '2008-11-08', 'E', 6, 2, 79.70, '2023-09-04', '0563-1010101', 'berkay.dogan@ogrenci.com'),
('2024KON002', '82828282828', 'Azra', 'Çakır', '2009-04-21', 'K', 6, 1, 94.60, '2024-09-02', '0563-2020202', 'azra.cakir@ogrenci.com'),
('2024KON003', '93939393939', 'Onur', 'Koç', '2007-02-14', 'E', 7, 3, 83.90, '2022-09-05', '0563-3030303', 'onur.koc@ogrenci.com'),
('2024ITU001', '04040404040', 'Elif', 'Sarı', '2008-06-27', 'K', 4, 2, 97.20, '2023-09-04', '0564-1010101', 'elif.sari@ogrenci.com'),
('2024ITU002', '16161616161', 'Barış', 'Güneş', '2009-09-10', 'E', 5, 1, 85.80, '2024-09-02', '0564-2020202', 'baris.gunes@ogrenci.com');

-- ============================================================
-- VERİ: dersler
-- ============================================================
INSERT INTO dersler (ders_kodu, ders_adi, bolum_id, ogretmen_id, kredi, sinif_seviyesi, zorunlu) VALUES
('MAT101', 'Matematik I', 1, 1, 4, 1, TRUE),
('FIZ101', 'Fizik I', 1, 2, 4, 1, TRUE),
('KIM101', 'Kimya I', 1, 3, 3, 1, TRUE),
('MAT201', 'Matematik II', 1, 1, 4, 2, TRUE),
('FIZ201', 'Fizik II', 1, 2, 4, 2, TRUE),
('TUR101', 'Türk Dili ve Edebiyatı', 2, 4, 3, 1, TRUE),
('TAR101', 'Tarih', 2, 5, 3, 1, TRUE),
('COG101', 'Coğrafya', 2, 5, 3, 1, TRUE),
('ING101', 'İngilizce I', 3, 6, 4, 1, TRUE),
('ING201', 'İngilizce II', 3, 6, 4, 2, TRUE),
('BIO101', 'Biyoloji', 10, 7, 3, 1, TRUE),
('BIL101', 'Bilgisayar Temelleri', 11, 10, 3, 1, TRUE),
('PRG101', 'Programlamaya Giriş', 11, 10, 4, 1, TRUE),
('WEB101', 'Web Tasarım', 11, 10, 3, 2, FALSE),
('VTB101', 'Veritabanı Yönetimi', 11, 10, 4, 2, TRUE);

-- ============================================================
-- VERİ: ogrenci_dersler (örnek kayıtlar)
-- ============================================================
INSERT INTO ogrenci_dersler (ogrenci_id, ders_id, donem, vize_notu, final_notu, harf_notu, gecti) VALUES
(1, 1, '2024-Güz', 85.00, 90.00, 'AA', TRUE),
(1, 2, '2024-Güz', 78.00, 82.00, 'BA', TRUE),
(1, 3, '2024-Güz', 92.00, 88.00, 'AA', TRUE),
(2, 1, '2024-Güz', 95.00, 98.00, 'AA', TRUE),
(2, 2, '2024-Güz', 88.00, 94.00, 'AA', TRUE),
(3, 1, '2023-Güz', 72.00, 75.00, 'BB', TRUE),
(3, 4, '2024-Güz', 80.00, 85.00, 'BA', TRUE),
(4, 1, '2023-Güz', 96.00, 98.00, 'AA', TRUE),
(4, 4, '2024-Güz', 94.00, 97.00, 'AA', TRUE),
(5, 1, '2024-Güz', 78.00, 84.00, 'BA', TRUE),
(19, 12, '2024-Güz', 98.00, 97.00, 'AA', TRUE),
(19, 13, '2024-Güz', 95.00, 96.00, 'AA', TRUE),
(20, 12, '2024-Güz', 85.00, 90.00, 'AA', TRUE),
(20, 14, '2024-Güz', 82.00, 87.00, 'BA', TRUE),
(21, 15, '2023-Güz', 79.00, 83.00, 'BA', TRUE),
(22, 12, '2024-Güz', 99.00, 99.00, 'AA', TRUE),
(22, 13, '2024-Güz', 97.00, 98.00, 'AA', TRUE),
(22, 15, '2024-Güz', 96.00, 99.00, 'AA', TRUE);

-- ============================================================
-- VİEW: Öğrenci özet görünümü
-- ============================================================
CREATE OR REPLACE VIEW v_ogrenci_ozet AS
SELECT
    o.ogrenci_no,
    o.ad,
    o.soyad,
    o.cinsiyet,
    o.sinif,
    o.ortalama,
    b.bolum_adi,
    ok.okul_adi,
    ok.sehir,
    o.email,
    o.telefon,
    o.kayit_tarihi
FROM ogrenciler o
JOIN bolumler b ON o.bolum_id = b.bolum_id
JOIN okullar ok ON b.okul_id = ok.okul_id
WHERE o.aktif = TRUE
ORDER BY ok.okul_adi, b.bolum_adi, o.soyad, o.ad;

SELECT 'Veritabanı başarıyla oluşturuldu! Tablolar: okullar, bolumler, ogretmenler, ogrenciler, dersler, ogrenci_dersler' AS durum;
