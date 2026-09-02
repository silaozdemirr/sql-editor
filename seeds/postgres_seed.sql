
CREATE DATABASE kitaplik_db;
\c kitaplik_db;

CREATE TABLE YAZARLAR (
  ID SERIAL PRIMARY KEY,
  AD VARCHAR(100) NOT NULL,
  ULKE VARCHAR(50)
);

CREATE TABLE KATEGORILER (
  ID SERIAL PRIMARY KEY,
  KATEGORI_ADI VARCHAR(50) NOT NULL
);

CREATE TABLE KITAPLAR (
  ID SERIAL PRIMARY KEY,
  KITAP_ADI VARCHAR(150) NOT NULL,
  YAZAR_ID INT REFERENCES YAZARLAR(ID),
  KATEGORI_ID INT REFERENCES KATEGORILER(ID),
  SAYFA_SAYISI INT,
  YAYIN_YILI INT
);

INSERT INTO YAZARLAR (AD, ULKE) VALUES 
('J.R.R. Tolkien', 'İngiltere'),
('George Orwell', 'İngiltere'),
('Sabahattin Ali', 'Türkiye'),
('Fyodor Dostoyevski', 'Rusya'),
('Franz Kafka', 'Çekya');

INSERT INTO KATEGORILER (KATEGORI_ADI) VALUES 
('Fantastik'), ('Bilim Kurgu'), ('Klasik'), ('Roman'), ('Kısa Öykü');

INSERT INTO KITAPLAR (KITAP_ADI, YAZAR_ID, KATEGORI_ID, SAYFA_SAYISI, YAYIN_YILI) VALUES 
('Yüzüklerin Efendisi', 1, 1, 1024, 1954),
('1984', 2, 2, 328, 1949),
('Hayvan Çiftliği', 2, 2, 112, 1945),
('Kürk Mantolu Madonna', 3, 4, 160, 1943),
('Suç ve Ceza', 4, 3, 687, 1866),
('Dönüşüm', 5, 5, 80, 1915);
