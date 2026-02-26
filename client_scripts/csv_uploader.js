// client_scripts/csv_uploader.js
// 시작하기 전에 
// 1. npm install axios csv-parser
// 2. file_path 본인의 경로로 바꿔주세요
const fs = require('fs');
const axios = require('axios');
const csv = require('csv-parser');

const FILE_PATH = 'C:/woori-workspace/98.data/card_data/edu_data_F.csv';
const TARGET_URL = 'http://localhost/api/upload';
const BATCH_SIZE = 10000;

async function sendBatch(data) {
    try {
        // CSV 그대로
        const csvContent = data.map(row => Object.values(row).join(',')).join('\n');
        
        await axios.post(TARGET_URL, csvContent, {
            headers: { 'Content-Type': 'text/csv' },
            timeout: 60000 
        });
        return true;
    } catch (error) {
        console.error('전송 실패:', error.message);
        return false;
    }
}

async function run() {
    let batch = [];
    let totalSent = 0;

    console.log('데이터 전송 시작...');

    const stream = fs.createReadStream(FILE_PATH).pipe(csv());

    for await (const row of stream) {
        batch.push(row);

        if (batch.length === BATCH_SIZE) {
            const success = await sendBatch(batch);
            if (success) {
                totalSent += batch.length;
                console.log(`[${new Date().toLocaleTimeString()}] 전송 완료: ${totalSent} 건`);
            }
            batch = []; 
        }
    }

    if (batch.length > 0) {
        await sendBatch(batch);
        console.log(`최종 완료: 총 ${totalSent + batch.length} 건`);
    }
}

run();