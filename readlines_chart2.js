const fs = require('fs');
const lines = fs.readFileSync('frontend/src/components/QueryResults.jsx', 'utf8').split('\n');
const idx = lines.findIndex(l => l.includes('const ChartRenderer'));
for(let i = idx; i <= idx + 25; i++) {
    console.log(`${i+1}: ${lines[i]}`);
}
