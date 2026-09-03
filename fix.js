const fs = require('fs');

let code = fs.readFileSync('frontend/src/components/SqlEditor.jsx', 'utf8');

// Add import
const importTarget = "import { executeQuery, explainQuery, manageTransaction, generateSqlWithAi, executeStreamQuery, cancelStreamQuery } from '../api/queryApi';";
const importReplacement = importTarget + "\nimport { getDatabases } from '../api/schemaApi';";
code = code.replace(importTarget, importReplacement);

const target = `    const saveScript = useCallback(() => {
    const executableSql = activeTab.query.trim();
    if (!executableSql) {
      updateTab(activeTabId, { notice: 'Kaydedilecek sorgu boş.' });
      return;
    }
    const name = window.prompt('Bu betik için bir ad girin:');
    if (!name) return;
    const existing = JSON.parse(localStorage.getItem('savedScripts') || '[]');
    existing.push({ name, query: executableSql, id: Date.now(), database: currentDatabase });
    localStorage.setItem('savedScripts', JSON.stringify(existing));
    window.dispatchEvent(new Event('savedScriptsUpdated'));
    updateTab(activeTabId, { notice: \`Betik '\${name}' olarak kaydedildi.\` });
  }, [activeTab.query, activeTabId, currentDatabase, updateTab]);`;

// Since there are formatting differences (like tabs vs spaces or single quotes vs double quotes or turkish chars), we will replace a smaller block:

code = code.replace("const saveScript = useCallback(() => {", "const saveScript = useCallback(async () => {");
code = code.replace("  }, [activeTab.query, activeTabId, currentDatabase, updateTab]);", "  }, [activeTab.query, activeTabId, currentDatabase, updateTab, connectionToken]);");

const pushTarget = "const existing = JSON.parse(localStorage.getItem('savedScripts') || '[]');\n    existing.push({ name, query: executableSql, id: Date.now(), database: currentDatabase });";
const pushReplacement = `let guessedDb = currentDatabase;
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

code = code.replace(pushTarget, pushReplacement);

const noticeTarget = "updateTab(activeTabId, { notice: `Betik '${name}' olarak kaydedildi.` });";
const noticeReplacement = "updateTab(activeTabId, { notice: `Betik '${name}' olarak '${targetDb}' altına kaydedildi.` });";
code = code.replace(noticeTarget, noticeReplacement);

fs.writeFileSync('frontend/src/components/SqlEditor.jsx', code, 'utf8');
console.log("SUCCESS");
