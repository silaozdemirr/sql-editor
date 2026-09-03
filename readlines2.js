const fs = require('fs');
const lines = fs.readFileSync('frontend/src/components/SqlEditor.jsx', 'utf8').split('\n');
for(let i = 385; i <= 405; i++) {
    console.log(`${i+1}: ${lines[i]}`);
}
