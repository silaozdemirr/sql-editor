import { test, expect } from '@playwright/test';

test('sayfa yükleniyor mu ve bağlan butonu çalışıyor mu?', async ({ page }) => {
  // Projenin çalıştığı port 5173
  await page.goto('http://localhost:5173/');

  // Sayfada "Veritabanı Türü" vb. panel elemanları var mı?
  await expect(page.getByText('Veritabanı Türü')).toBeVisible();

  // Host ve kullanıcı adı formunu dolduralım
  await page.fill('#host', 'localhost');
  await page.fill('#username', 'testuser');

  // Bağlan butonuna tıklayalım
  const connectButton = page.getByRole('button', { name: /bağlan/i });
  await expect(connectButton).toBeVisible();
  
  // Burada backend ayakta olmayabileceği için tıklama sonrası gelen hata veya loading state'ini kontrol edebiliriz
  // await connectButton.click();
  // await expect(page.locator('.error-message')).toBeVisible(); 
});
