const fs = require('fs');
const lines = fs.readFileSync('frontend/src/components/QueryResults.jsx', 'utf8').split('\n');
for(let i = 240; i <= 250; i++) {
    console.log(`${i+1}: ${lines[i]}`);
}
