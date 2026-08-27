import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export function generateHtmlReport(results, deviceName = 'Android Emulator') {
  const total = results.length;
  const passed = results.filter(r => r.status === 'PASS' || r.status === 'Passed').length;
  const failed = results.filter(r => r.status === 'FAIL' || r.status === 'Failed').length;
  const skipped = results.filter(r => r.status === 'SKIPPED' || r.status === 'Skipped').length;
  const passRate = total > 0 ? (passed / total) * 100 : 0;
  const totalDuration = results.reduce((acc, r) => acc + r.duration, 0);

  // Group calculations
  const fTotal = results.filter(r => r.id.startsWith('AND-F')).length;
  const fPassed = results.filter(r => (r.id.startsWith('AND-F') && (r.status === 'PASS' || r.status === 'Passed'))).length;
  
  const uiTotal = results.filter(r => r.id.startsWith('AND-U')).length;
  const uiPassed = results.filter(r => (r.id.startsWith('AND-U') && (r.status === 'PASS' || r.status === 'Passed'))).length;

  const sTotal = results.filter(r => r.id.startsWith('AND-S')).length;
  const sPassed = results.filter(r => (r.id.startsWith('AND-S') && (r.status === 'PASS' || r.status === 'Passed'))).length;

  const fPassRate = fTotal > 0 ? (fPassed / fTotal) * 100 : 0;
  const uiPassRate = uiTotal > 0 ? (uiPassed / uiTotal) * 100 : 0;
  const sPassRate = sTotal > 0 ? (sPassed / sTotal) * 100 : 0;

  const failedTestsList = results.filter(r => r.status === 'FAIL' || r.status === 'Failed');
  let failedRowsHtml = '';
  failedTestsList.forEach(t => {
    failedRowsHtml += `
      <div class="fail-card">
        <div class="fail-header">
          <span class="fail-id">${t.id}</span>
          <span class="fail-name">${t.name}</span>
          <span class="fail-category">${t.category}</span>
        </div>
        <div class="fail-body">
          <p><strong>Steps:</strong> ${t.steps || 'N/A'}</p>
          <p><strong>Expected:</strong> ${t.expectedResult || 'N/A'}</p>
          <p><strong>Actual:</strong> ${t.actualResult || 'N/A'}</p>
          <p class="error-msg"><strong>Error Message:</strong> ${t.error || 'N/A'}</p>
        </div>
      </div>
    `;
  });

  if (failedRowsHtml === '') {
    failedRowsHtml = '<p class="no-failures">🎉 Perfect run! No failures detected.</p>';
  }

  const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Mobile Appium E2E Test Report</title>
  <style>
    :root {
      --bg-dark: #0f172a;
      --card-bg: #1e293b;
      --text-main: #f8fafc;
      --text-muted: #94a3b8;
      --primary: #0284c7;
      --success: #10b981;
      --error: #ef4444;
      --warning: #f59e0b;
      --border: #334155;
    }
    
    body {
      background-color: var(--bg-dark);
      color: var(--text-main);
      font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
      margin: 0;
      padding: 24px;
    }
    
    .container {
      max-width: 1200px;
      margin: 0 auto;
    }
    
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border);
      padding-bottom: 16px;
      margin-bottom: 24px;
    }
    
    h1 {
      margin: 0;
      font-size: 28px;
      font-weight: 800;
      background: linear-gradient(to right, #38bdf8, #34d399);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    
    .meta-info {
      font-size: 14px;
      color: var(--text-muted);
      text-align: right;
    }
    
    .grid-stats {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    
    .stat-card {
      background-color: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 20px;
      text-align: center;
    }
    
    .stat-val {
      font-size: 24px;
      font-weight: 800;
      margin-bottom: 4px;
    }
    
    .stat-lbl {
      font-size: 12px;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    
    .passed { color: var(--success); }
    .failed { color: var(--error); }
    .skipped { color: var(--warning); }
    
    .sections-container {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
      margin-bottom: 24px;
    }
    
    @media (max-width: 768px) {
      .sections-container {
        grid-template-columns: 1fr;
      }
    }
    
    .panel {
      background-color: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 16px;
      padding: 24px;
    }
    
    h3 {
      margin-top: 0;
      margin-bottom: 20px;
      font-size: 18px;
      font-weight: 700;
      color: #38bdf8;
      border-bottom: 1px dashed var(--border);
      padding-bottom: 8px;
    }
    
    .group-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    }
    
    .group-name {
      font-weight: 600;
      font-size: 14px;
    }
    
    .group-stats {
      font-size: 14px;
      color: var(--text-muted);
      text-align: right;
    }
    
    .group-badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 20px;
      font-weight: 700;
      font-size: 12px;
      margin-left: 10px;
    }
    
    .badge-pass { background-color: rgba(16, 185, 129, 0.15); color: var(--success); }
    
    /* Failure Details */
    .fail-card {
      background: rgba(239, 68, 68, 0.03);
      border: 1px solid rgba(239, 68, 68, 0.2);
      border-radius: 10px;
      padding: 16px;
      margin-bottom: 16px;
    }
    
    .fail-header {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      margin-bottom: 10px;
      border-bottom: 1px solid rgba(239, 68, 68, 0.1);
      padding-bottom: 6px;
    }
    
    .fail-id {
      color: var(--error);
      font-weight: bold;
    }
    
    .fail-name {
      font-weight: 600;
      flex: 1;
      margin-left: 10px;
    }
    
    .fail-category {
      color: var(--text-muted);
    }
    
    .fail-body p {
      margin: 4px 0;
      font-size: 13px;
    }
    
    .error-msg {
      color: #fda4af;
      background: rgba(239, 68, 68, 0.1);
      padding: 8px 12px;
      border-radius: 6px;
      font-family: monospace;
      margin-top: 8px !important;
      word-break: break-all;
    }
    
    .no-failures {
      text-align: center;
      padding: 20px;
      color: var(--success);
      font-weight: bold;
    }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div>
        <h1>Mobile Appium E2E Report</h1>
        <p style="margin:4px 0 0 0; color:var(--text-muted); font-size:14px;">Android Mobile Testing execution summary</p>
      </div>
      <div class="meta-info">
        <div>Date: ${new Date().toLocaleDateString()}</div>
        <div>Time: ${new Date().toLocaleTimeString()}</div>
        <div>Target Device: ${deviceName}</div>
      </div>
    </header>

    <div class="grid-stats">
      <div class="stat-card">
        <div class="stat-val" style="color:var(--primary);">${total}</div>
        <div class="stat-lbl">Total Tests</div>
      </div>
      <div class="stat-card">
        <div class="stat-val passed">${passed}</div>
        <div class="stat-lbl">Passed</div>
      </div>
      <div class="stat-card">
        <div class="stat-val failed">${failed}</div>
        <div class="stat-lbl">Failed</div>
      </div>
      <div class="stat-card">
        <div class="stat-val skipped">${skipped}</div>
        <div class="stat-lbl">Skipped</div>
      </div>
      <div class="stat-card">
        <div class="stat-val" style="color: #c084fc;">${passRate.toFixed(2)}%</div>
        <div class="stat-lbl">Pass Percentage</div>
      </div>
      <div class="stat-card">
        <div class="stat-val" style="color:#2dd4bf;">${(totalDuration / 1000).toFixed(2)}s</div>
        <div class="stat-lbl">Duration</div>
      </div>
    </div>

    <div class="sections-container">
      <div class="panel">
        <h3>Group Breakdown Metrics</h3>
        <div class="group-row">
          <span class="group-name">Group 1 Functional</span>
          <div class="group-stats">
            <span>${fPassed}/${fTotal} Passed</span>
            <span class="group-badge badge-pass">${fPassRate.toFixed(1)}%</span>
          </div>
        </div>
        <div class="group-row">
          <span class="group-name">Group 2 UI & Integration</span>
          <div class="group-stats">
            <span>${uiPassed}/${uiTotal} Passed</span>
            <span class="group-badge badge-pass">${uiPassRate.toFixed(1)}%</span>
          </div>
        </div>
        <div class="group-row">
          <span class="group-name">Group 3 Security & Regression</span>
          <div class="group-stats">
            <span>${sPassed}/${sTotal} Passed</span>
            <span class="group-badge badge-pass">${sPassRate.toFixed(1)}%</span>
          </div>
        </div>
      </div>

      <div class="panel" style="max-height: 400px; overflow-y: auto;">
        <h3>Execution Logs & Errors</h3>
        ${failedRowsHtml}
      </div>
    </div>
  </div>
</body>
</html>`;

  const reportsDir = path.join(__dirname, '../reports');
  if (!fs.existsSync(reportsDir)) {
    fs.mkdirSync(reportsDir, { recursive: true });
  }

  const mainPath = path.join(reportsDir, 'index.html');
  fs.writeFileSync(mainPath, htmlContent, 'utf8');
  console.log(`HTML Report created at: ${mainPath}`);

  // Save history backup
  const dateStr = new Date().toISOString().replace(/T/, '_').replace(/:/g, '-').split('.')[0];
  const historyDir = path.join(__dirname, '../reports/history', dateStr);
  if (!fs.existsSync(historyDir)) {
    fs.mkdirSync(historyDir, { recursive: true });
  }
  const historyPath = path.join(historyDir, 'index.html');
  fs.writeFileSync(historyPath, htmlContent, 'utf8');
  console.log(`HTML Historical Backup created at: ${historyPath}`);
}
