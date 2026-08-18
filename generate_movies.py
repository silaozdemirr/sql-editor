import random
from datetime import datetime, timedelta

def generate_movies_sql():
    sql = "SET NAMES utf8mb4;\n"
    sql += "CREATE DATABASE IF NOT EXISTS filmler_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;\n"
    sql += "USE filmler_db;\n\n"
    
    # Drop tables if exist
    sql += "DROP TABLE IF EXISTS filmler;\n"
    sql += "DROP TABLE IF EXISTS yonetmenler;\n"
    sql += "DROP TABLE IF EXISTS kategoriler;\n\n"

    # Create tables
    sql += """
CREATE TABLE kategoriler (
  id INT NOT NULL AUTO_INCREMENT,
  kategori_adi VARCHAR(50) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE yonetmenler (
  id INT NOT NULL AUTO_INCREMENT,
  isim VARCHAR(100) NOT NULL,
  ulke VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE filmler (
  id INT NOT NULL AUTO_INCREMENT,
  film_adi VARCHAR(150) NOT NULL,
  yayin_yili INT DEFAULT NULL,
  imdb_puani DECIMAL(3,1) DEFAULT NULL,
  yonetmen_id INT DEFAULT NULL,
  kategori_id INT DEFAULT NULL,
  izlenme_sayisi INT DEFAULT 0,
  PRIMARY KEY (id),
  FOREIGN KEY (yonetmen_id) REFERENCES yonetmenler(id),
  FOREIGN KEY (kategori_id) REFERENCES kategoriler(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"""
    sql += "\n"

    # Seed Kategoriler (10 records)
    kategoriler = ["Aksiyon", "Bilim Kurgu", "Dram", "Komedi", "Korku", "Gerilim", "Romantik", "Animasyon", "Belgesel", "Tarihi"]
    sql += "INSERT INTO kategoriler (kategori_adi) VALUES\n"
    sql += ",\n".join([f"('{k}')" for k in kategoriler]) + ";\n\n"

    # Seed Yonetmenler (20 records)
    yonetmenler = [
        ("Christopher Nolan", "İngiltere"), ("Quentin Tarantino", "ABD"), ("Steven Spielberg", "ABD"),
        ("Martin Scorsese", "ABD"), ("Nuri Bilge Ceylan", "Türkiye"), ("Zeki Demirkubuz", "Türkiye"),
        ("Yavuz Turgul", "Türkiye"), ("Stanley Kubrick", "ABD"), ("Alfred Hitchcock", "İngiltere"),
        ("David Fincher", "ABD"), ("Ridley Scott", "İngiltere"), ("James Cameron", "Kanada"),
        ("Peter Jackson", "Yeni Zelanda"), ("Bong Joon-ho", "Güney Kore"), ("Hayao Miyazaki", "Japonya"),
        ("Akira Kurosawa", "Japonya"), ("Çağan Irmak", "Türkiye"), ("Pedro Almodóvar", "İspanya"),
        ("Guillermo del Toro", "Meksika"), ("Denis Villeneuve", "Kanada")
    ]
    sql += "INSERT INTO yonetmenler (isim, ulke) VALUES\n"
    sql += ",\n".join([f"('{y[0]}', '{y[1]}')" for y in yonetmenler]) + ";\n\n"

    # Seed Filmler (150 records)
    film_isim_kelimeler1 = ["Büyük", "Karanlık", "Gizemli", "Kayıp", "Son", "Sessiz", "Kanlı", "Eski", "Yeni", "Sonsuz", "Gizli", "Parlak", "Tehlikeli", "Yalnız", "Unutulmaz"]
    film_isim_kelimeler2 = ["Yolculuk", "Sır", "Zaman", "Rüya", "Adam", "Kadın", "Gün", "Gece", "Umut", "Şehir", "Oyun", "Savaş", "Aşk", "Macera", "İhanet"]
    
    sql += "INSERT INTO filmler (film_adi, yayin_yili, imdb_puani, yonetmen_id, kategori_id, izlenme_sayisi) VALUES\n"
    film_values = []
    
    # Let's add some famous movies first
    famous_movies = [
        ("Inception", 2010, 8.8, 1, 2, 2500000),
        ("Pulp Fiction", 1994, 8.9, 2, 6, 2100000),
        ("Schindler'in Listesi", 1993, 9.0, 3, 10, 1800000),
        ("Kış Uykusu", 2014, 8.1, 5, 3, 500000),
        ("Eşkıya", 1996, 8.2, 7, 3, 1200000),
        ("Fight Club", 1999, 8.8, 10, 3, 2300000),
        ("Matrix", 1999, 8.7, 10, 2, 2200000),
        ("Avatar", 2009, 7.8, 12, 2, 3000000),
        ("Yüzüklerin Efendisi: Yüzük Kardeşliği", 2001, 8.8, 13, 1, 2400000),
        ("Parazit", 2019, 8.5, 14, 6, 1500000)
    ]
    
    for f in famous_movies:
        isim_escaped = f[0].replace("'", "''")
        film_values.append(f"('{isim_escaped}', {f[1]}, {f[2]}, {f[3]}, {f[4]}, {f[5]})")
        
    # Generate the rest up to 150
    for i in range(11, 151):
        isim = f"{random.choice(film_isim_kelimeler1)} {random.choice(film_isim_kelimeler2)}"
        if random.random() > 0.5:
            isim += f" {random.randint(1, 5)}" # Like "Karanlık Rüya 2"
            
        yayin_yili = random.randint(1970, 2024)
        imdb = round(random.uniform(4.0, 9.5), 1)
        yon_id = random.randint(1, 20)
        kat_id = random.randint(1, 10)
        izlenme = random.randint(1000, 5000000)
        
        isim_escaped = isim.replace("'", "''")
        film_values.append(f"('{isim_escaped}', {yayin_yili}, {imdb}, {yon_id}, {kat_id}, {izlenme})")
        
    sql += ",\n".join(film_values) + ";\n"

    with open("movies_seed.sql", "w", encoding="utf-8") as f:
        f.write(sql)

generate_movies_sql()
