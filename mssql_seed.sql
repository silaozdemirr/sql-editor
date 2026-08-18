
CREATE DATABASE E_TICARET;
GO

USE E_TICARET;
GO

CREATE TABLE KATEGORILER (
  ID INT IDENTITY(1,1) PRIMARY KEY,
  KATEGORI_ADI NVARCHAR(50) NOT NULL
);

CREATE TABLE MARKALAR (
  ID INT IDENTITY(1,1) PRIMARY KEY,
  MARKA_ADI NVARCHAR(100) NOT NULL,
  ULKE NVARCHAR(50)
);

CREATE TABLE URUNLER (
  ID INT IDENTITY(1,1) PRIMARY KEY,
  URUN_ADI NVARCHAR(150) NOT NULL,
  FIYAT DECIMAL(10,2),
  STOK_ADEDI INT,
  MARKA_ID INT FOREIGN KEY REFERENCES MARKALAR(ID),
  KATEGORI_ID INT FOREIGN KEY REFERENCES KATEGORILER(ID),
  GORUNTULENME_SAYISI INT DEFAULT 0
);
GO

INSERT INTO KATEGORILER (KATEGORI_ADI) VALUES 
('Elektronik'), ('Giyim'), ('Ev & Yaşam'), ('Kozmetik'), ('Spor'),
('Kitap'), ('Oyuncak'), ('Oto Aksesuar'), ('Süpermarket'), ('Petshop');

INSERT INTO MARKALAR (MARKA_ADI, ULKE) VALUES 
('Apple', 'ABD'), ('Samsung', 'Güney Kore'), ('Sony', 'Japonya'),
('Nike', 'ABD'), ('Adidas', 'Almanya'), ('Bosch', 'Almanya'),
('Philips', 'Hollanda'), ('Loreal', 'Fransa'), ('LC Waikiki', 'Türkiye'),
('Xiaomi', 'Çin');

INSERT INTO URUNLER (URUN_ADI, FIYAT, STOK_ADEDI, MARKA_ID, KATEGORI_ID, GORUNTULENME_SAYISI) VALUES 
('iPhone 15 Pro Max', 75000.00, 50, 1, 1, 150000),
('Galaxy S24 Ultra', 68000.00, 75, 2, 1, 120000),
('PlayStation 5', 25000.00, 30, 3, 1, 200000),
('Air Max 2024', 4500.00, 150, 4, 2, 80000),
('Ultraboost Light', 4800.00, 120, 5, 2, 75000),
('Serie 8 Çamaşır Makinesi', 22000.00, 40, 6, 3, 45000),
('Airfryer XXL', 6500.00, 85, 7, 3, 110000),
('Revitalift Lazer Krem', 450.00, 300, 8, 4, 25000),
('Erkek Kışlık Mont', 1200.00, 200, 9, 2, 60000),
('Robot Süpürge S10+', 12500.00, 60, 10, 1, 95000);
GO
