const fs = require('fs');
let code = fs.readFileSync('frontend/src/components/SqlEditor.jsx', 'utf8');

const regexPush = /const existing = JSON\.parse\(localStorage\.getItem\('savedScripts'\) \|\| '\[\]'\);\s*existing\.push\(\{ name, query: executableSql, id: Date\.now\(\), database: currentDatabase \}\);/;

const replacement = `let guessedDb = currentDatabase;
    const dbMatch = executableSql.match(/(?:from|join|update|into|table)\\s+[\`'"]?([a-zA-Z0-9_]+)[\`'"]?\\./i);
    if (dbMatch && dbMatch[1]) {
      guessedDb = dbMatch[1];
    }
    
    let targetDb = window.prompt('Hangi veritabanı altına kaydedilsin?', guessedDb);
    if (targetDb === null) return;
    
    try {
        const validDatabases = await getDatabases(connectionToken);
        while (validDatabases && validDatabases.length > 0 && !validDatabases.includes(targetDb)) {
            window.alert(\`Hata: '\${targetDb}' adında bir veritabanı bulunamadı!\\nMevcut veritabanları:\\n\${validDatabases.join(', ')}\`);
            targetDb = window.prompt('Lütfen geçerli bir veritabanı adı girin:', guessedDb);
            if (targetDb === null) return;
        }
    } catch (e) {
        console.error("DB listesi alınamadı, doğrulama atlandı.");
    }

    const existing = JSON.parse(localStorage.getItem('savedScripts') || '[]');
    existing.push({ name, query: executableSql, id: Date.now(), database: targetDb });`;

code = code.replace(regexPush, replacement);

fs.writeFileSync('frontend/src/components/SqlEditor.jsx', code, 'utf8');
console.log("SUCCESS");
