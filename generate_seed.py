import random
from datetime import datetime, timedelta

def generate_sql():
    sql = "SET NAMES utf8mb4;\n"
    sql += "USE hastane_db;\n\n"
    
    # Delete previous seeded data (IDs > 4)
    sql += "DELETE FROM randevular WHERE id > 4;\n"
    sql += "DELETE FROM doktorlar WHERE id > 4;\n"
    sql += "DELETE FROM hastalar WHERE id > 4;\n"
    sql += "DELETE FROM bolumler WHERE id > 4;\n\n"

    # 90 bolumler
    bolum_adlari = ["Kardiyoloji", "Nöroloji", "Dahiliye", "Ortopedi", "Cildiye", "Göz", "KBB", "Psikiyatri", "Üroloji", "Genel Cerrahi"]
    sql += "INSERT IGNORE INTO bolumler (id, bolum_adi) VALUES\n"
    bolum_values = []
    for i in range(5, 95):
        name = f"{bolum_adlari[i % len(bolum_adlari)]} Polikliniği {i}"
        bolum_values.append(f"({i}, '{name}')")
    sql += ",\n".join(bolum_values) + ";\n\n"

    # 90 doktorlar
    isimler = ["Ahmet", "Mehmet", "Ayşe", "Fatma", "Ali", "Veli", "Canan", "Kemal", "Zeynep", "Hasan", "Hüseyin", "Emine", "Murat", "Burcu", "Deniz"]
    soyadlar = ["Yılmaz", "Kaya", "Demir", "Çelik", "Şahin", "Yıldız", "Öztürk", "Aydın", "Özdemir", "Arslan", "Doğan", "Kılıç", "Aslan", "Çetin"]
    uzmanliklar = ["Uzman", "Asistan", "Prof. Dr.", "Doç. Dr.", "Operatör", "Pratisyen"]
    
    sql += "INSERT IGNORE INTO doktorlar (id, isim, uzmanlik_alani, bolum_id) VALUES\n"
    doktor_values = []
    for i in range(5, 95):
        isim = f"{random.choice(isimler)} {random.choice(soyadlar)}"
        uzmanlik = random.choice(uzmanliklar)
        bolum_id = random.randint(1, 94)
        doktor_values.append(f"({i}, '{isim}', '{uzmanlik}', {bolum_id})")
    sql += ",\n".join(doktor_values) + ";\n\n"

    # 90 hastalar
    kan_gruplari = ["A+", "A-", "B+", "B-", "AB+", "AB-", "0+", "0-"]
    sql += "INSERT IGNORE INTO hastalar (id, isim, tc_kimlik, dogum_tarihi, kan_grubu) VALUES\n"
    hasta_values = []
    start_date = datetime(1950, 1, 1)
    for i in range(5, 95):
        isim = f"{random.choice(isimler)} {random.choice(soyadlar)}"
        tc = f"1{''.join([str(random.randint(0, 9)) for _ in range(10)])}"
        dt = start_date + timedelta(days=random.randint(0, 20000))
        kg = random.choice(kan_gruplari)
        hasta_values.append(f"({i}, '{isim}', '{tc}', '{dt.strftime('%Y-%m-%d')}', '{kg}')")
    sql += ",\n".join(hasta_values) + ";\n\n"

    # 90 randevular
    durumlar = ["Bekliyor", "Tamamlandı", "İptal", "Ertelendi"]
    sql += "INSERT IGNORE INTO randevular (id, hasta_id, doktor_id, randevu_tarihi, durum) VALUES\n"
    randevu_values = []
    base_randevu = datetime(2023, 1, 1, 9, 0, 0)
    for i in range(5, 95):
        hasta_id = random.randint(1, 94)
        doktor_id = random.randint(1, 94)
        tarih = base_randevu + timedelta(days=random.randint(0, 365), hours=random.randint(0, 8))
        durum = random.choice(durumlar)
        randevu_values.append(f"({i}, {hasta_id}, {doktor_id}, '{tarih.strftime('%Y-%m-%d %H:%M:%S')}', '{durum}')")
    sql += ",\n".join(randevu_values) + ";\n"

    with open("seed.sql", "w", encoding="utf-8") as f:
        f.write(sql)

generate_sql()
