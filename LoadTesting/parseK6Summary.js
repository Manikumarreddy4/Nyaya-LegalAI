import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import ExcelJS from 'exceljs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function getMetricValue(metricObj, key) {
  if (!metricObj) return null;
  if (metricObj.values && metricObj.values[key] !== undefined) {
    return metricObj.values[key];
  }
  if (metricObj[key] !== undefined) {
    return metricObj[key];
  }
  return null;
}

async function generateExcelReport(reportData, outputPath) {
  const workbook = new ExcelJS.Workbook();

  // Sheet 1: Load Test Summary
  const summarySheet = workbook.addWorksheet('Load Test Summary');
  summarySheet.views = [{ showGridLines: true }];

  summarySheet.mergeCells('A1:B1');
  const title1 = summarySheet.getCell('A1');
  title1.value = 'API Load Testing Summary';
  title1.font = { name: 'Segoe UI', size: 14, bold: true, color: { argb: 'FFFFFF' } };
  title1.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '4F46E5' } };
  title1.alignment = { vertical: 'middle', horizontal: 'center' };
  summarySheet.getRow(1).height = 35;

  const header1 = summarySheet.getRow(2);
  header1.values = ['Metric', 'Value'];
  header1.height = 24;
  header1.eachCell(cell => {
    cell.font = { name: 'Segoe UI', bold: true, size: 10, color: { argb: 'FFFFFF' } };
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '1E293B' } };
    cell.border = { top: { style: 'thin' }, left: { style: 'thin' }, bottom: { style: 'thin' }, right: { style: 'thin' } };
  });

  const successRate = 100 - reportData.failureRate;

  const metricsRows = [
    ['Virtual Users', `${reportData.vus} VUs`],
    ['Test Duration', `${reportData.duration}`],
    ['Total Requests', `${reportData.totalRequests} requests`],
    ['Requests Per Second', `${reportData.rps.toFixed(2)} req/sec`],
    ['Average Response Time', `${reportData.avgDuration.toFixed(2)} ms`],
    ['Minimum Response Time', `${reportData.minDuration.toFixed(2)} ms`],
    ['Maximum Response Time', `${reportData.maxDuration.toFixed(2)} ms`],
    ['p95 Response Time', `${reportData.p95Duration.toFixed(2)} ms`],
    ['Failed Requests', `${reportData.failedRequests} requests`],
    ['Failure Rate', `${reportData.failureRate.toFixed(2)}%`],
    ['Successful Requests', `${reportData.totalRequests - reportData.failedRequests} requests`],
    ['Success Rate', `${successRate.toFixed(2)}%`]
  ];

  metricsRows.forEach((r, idx) => {
    const rowNum = 3 + idx;
    const currRow = summarySheet.getRow(rowNum);
    currRow.values = [r[0], r[1]];
    currRow.height = 20;
    currRow.eachCell(cell => {
      cell.font = { name: 'Segoe UI', size: 10 };
      cell.border = {
        top: { style: 'thin', color: { argb: 'E2E8F0' } },
        left: { style: 'thin', color: { argb: 'E2E8F0' } },
        bottom: { style: 'thin', color: { argb: 'E2E8F0' } },
        right: { style: 'thin', color: { argb: 'E2E8F0' } }
      };
    });
  });
  summarySheet.columns = [{ width: 30 }, { width: 25 }];

  // Sheet 2: Performance Analysis
  const analysisSheet = workbook.addWorksheet('Performance Analysis');
  analysisSheet.views = [{ showGridLines: true }];

  analysisSheet.mergeCells('A1:D1');
  const title2 = analysisSheet.getCell('A1');
  title2.value = 'Metric Threshold & SLA Verification';
  title2.font = { name: 'Segoe UI', size: 14, bold: true, color: { argb: 'FFFFFF' } };
  title2.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '0F172A' } };
  title2.alignment = { vertical: 'middle', horizontal: 'center' };
  analysisSheet.getRow(1).height = 35;

  const header2 = analysisSheet.getRow(2);
  header2.values = ['Metric', 'Result', 'Expected Threshold', 'Status'];
  header2.height = 24;
  header2.eachCell(cell => {
    cell.font = { name: 'Segoe UI', bold: true, size: 10, color: { argb: 'FFFFFF' } };
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '1E293B' } };
    cell.border = { top: { style: 'thin' }, left: { style: 'thin' }, bottom: { style: 'thin' }, right: { style: 'thin' } };
  });

  const p95Status = reportData.p95Duration < 1500 ? 'PASS' : 'FAIL';
  const failStatus = reportData.failureRate < 5 ? 'PASS' : 'FAIL';

  const analysisRows = [
    ['p95 Response Time', `${reportData.p95Duration.toFixed(2)} ms`, '< 1500.00 ms', p95Status],
    ['Failure Rate', `${reportData.failureRate.toFixed(2)}%`, '< 5.00%', failStatus]
  ];

  analysisRows.forEach((r, idx) => {
    const rowNum = 3 + idx;
    const currRow = analysisSheet.getRow(rowNum);
    currRow.values = [r[0], r[1], r[2], r[3]];
    currRow.height = 22;
    currRow.eachCell((cell, colIndex) => {
      cell.font = { name: 'Segoe UI', size: 10 };
      cell.border = {
        top: { style: 'thin', color: { argb: 'E2E8F0' } },
        left: { style: 'thin', color: { argb: 'E2E8F0' } },
        bottom: { style: 'thin', color: { argb: 'E2E8F0' } },
        right: { style: 'thin', color: { argb: 'E2E8F0' } }
      };

      if (colIndex === 4) {
        if (cell.value === 'PASS') {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'DCFCE7' } };
          cell.font = { name: 'Segoe UI', size: 10, bold: true, color: { argb: '166534' } };
        } else {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FEE2E2' } };
          cell.font = { name: 'Segoe UI', size: 10, bold: true, color: { argb: '991B1B' } };
        }
      }
    });
  });
  analysisSheet.columns = [{ width: 28 }, { width: 20 }, { width: 24 }, { width: 16 }];

  // Sheet 3: Test Configuration
  const configSheet = workbook.addWorksheet('Test Configuration');
  configSheet.views = [{ showGridLines: true }];

  configSheet.mergeCells('A1:B1');
  const title3 = configSheet.getCell('A1');
  title3.value = 'Load Testing Settings';
  title3.font = { name: 'Segoe UI', size: 14, bold: true, color: { argb: 'FFFFFF' } };
  title3.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '1E293B' } };
  title3.alignment = { vertical: 'middle', horizontal: 'center' };
  configSheet.getRow(1).height = 35;

  const configRows = [
    ['Test Type', 'Baseline / Load Testing'],
    ['Virtual Users', '100 VUs'],
    ['Duration', '1 Minute'],
    ['Backend URL', reportData.targetUrl],
    ['Test Date', new Date().toLocaleDateString()],
    ['Environment', 'QA-Performance']
  ];

  configRows.forEach((r, idx) => {
    const rowNum = 3 + idx;
    const currRow = configSheet.getRow(rowNum);
    currRow.values = [r[0], r[1]];
    currRow.height = 20;
    currRow.getCell(1).font = { name: 'Segoe UI', bold: true };
    currRow.getCell(2).font = { name: 'Segoe UI' };
    currRow.eachCell(cell => {
      cell.border = {
        top: { style: 'thin', color: { argb: 'E2E8F0' } },
        left: { style: 'thin', color: { argb: 'E2E8F0' } },
        bottom: { style: 'thin', color: { argb: 'E2E8F0' } },
        right: { style: 'thin', color: { argb: 'E2E8F0' } }
      };
    });
  });
  configSheet.columns = [{ width: 25 }, { width: 45 }];

  await workbook.xlsx.writeFile(outputPath);
  console.log(`Excel sheet saved to: ${outputPath}`);
}

function generateHtmlReport(reportData, outputPath) {
  const successRate = 100 - reportData.failureRate;
  const p95Status = reportData.p95Duration < 1500 ? '✅ PASS' : '❌ FAIL';
  const failStatus = reportData.failureRate < 5 ? '✅ PASS' : '❌ FAIL';

  const htmlContent = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>API Performance Load Test Report</title>
  <style>
    :root {
      --bg-dark: #0f172a;
      --card-bg: #1e293b;
      --text-main: #f8fafc;
      --text-muted: #94a3b8;
      --primary: #4f46e5;
      --success: #10b981;
      --error: #ef4444;
      --border: #334155;
    }
    
    body {
      background-color: var(--bg-dark);
      color: var(--text-main);
      font-family: 'Segoe UI', system-ui, sans-serif;
      margin: 0;
      padding: 24px;
    }
    
    .container {
      max-width: 1000px;
      margin: 0 auto;
    }
    
    header {
      border-bottom: 1px solid var(--border);
      padding-bottom: 16px;
      margin-bottom: 24px;
    }
    
    h1 {
      margin: 0;
      font-size: 28px;
      font-weight: 800;
      color: #818cf8;
    }
    
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    
    .card {
      background-color: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 20px;
      text-align: center;
    }
    
    .card-val {
      font-size: 28px;
      font-weight: 800;
      margin-bottom: 4px;
    }
    
    .card-lbl {
      font-size: 12px;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    
    .section-title {
      font-size: 18px;
      font-weight: 700;
      color: #818cf8;
      margin-bottom: 16px;
    }
    
    table {
      width: 100%;
      border-collapse: collapse;
      background-color: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 12px;
      overflow: hidden;
      margin-bottom: 24px;
    }
    
    th, td {
      padding: 12px 16px;
      text-align: left;
      border-bottom: 1px solid var(--border);
    }
    
    th {
      background-color: rgba(255, 255, 255, 0.03);
      font-weight: 700;
      color: var(--text-main);
    }
    
    td {
      font-size: 14px;
    }
    
    .status-pass { color: var(--success); font-weight: bold; }
    .status-fail { color: var(--error); font-weight: bold; }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <h1>k6 API Load Testing Performance Report</h1>
      <p style="margin:4px 0 0 0; color:var(--text-muted); font-size:14px;">Baseline Verification SLA Checklist</p>
    </header>

    <div class="stats-grid">
      <div class="card">
        <div class="card-val" style="color:var(--primary);">100 VUs</div>
        <div class="card-lbl">Virtual Users</div>
      </div>
      <div class="card">
        <div class="card-val" style="color:#2dd4bf;">${reportData.rps.toFixed(1)}/s</div>
        <div class="card-lbl">Throughput (RPS)</div>
      </div>
      <div class="card">
        <div class="card-val" style="color: #c084fc;">${reportData.p95Duration.toFixed(1)}ms</div>
        <div class="card-lbl">p95 Latency</div>
      </div>
      <div class="card">
        <div class="card-val" style="color:${reportData.failureRate < 5 ? 'var(--success)' : 'var(--error)'};">${reportData.failureRate.toFixed(2)}%</div>
        <div class="card-lbl">Failure Rate</div>
      </div>
    </div>

    <div class="section-title">Performance Criteria SLA Checks</div>
    <table>
      <thead>
        <tr>
          <th>Metric Name</th>
          <th>SLA Threshold</th>
          <th>Measured Value</th>
          <th>Verification Status</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td><strong>p95 Response Time</strong></td>
          <td>&lt; 1500.00 ms</td>
          <td>${reportData.p95Duration.toFixed(2)} ms</td>
          <td class="${reportData.p95Duration < 1500 ? 'status-pass' : 'status-fail'}">${p95Status}</td>
        </tr>
        <tr>
          <td><strong>Request Failure Rate</strong></td>
          <td>&lt; 5.00%</td>
          <td>${reportData.failureRate.toFixed(2)}%</td>
          <td class="${reportData.failureRate < 5 ? 'status-pass' : 'status-fail'}">${failStatus}</td>
        </tr>
      </tbody>
    </table>

    <div class="section-title">Execution Details</div>
    <table>
      <tbody>
        <tr>
          <td><strong>Target Backend URL</strong></td>
          <td>${reportData.targetUrl}</td>
        </tr>
        <tr>
          <td><strong>Total Requests Dispatched</strong></td>
          <td>${reportData.totalRequests} requests</td>
        </tr>
        <tr>
          <td><strong>Successful Requests</strong></td>
          <td>${reportData.totalRequests - reportData.failedRequests} requests (${successRate.toFixed(2)}%)</td>
        </tr>
        <tr>
          <td><strong>Average Latency</strong></td>
          <td>${reportData.avgDuration.toFixed(2)} ms</td>
        </tr>
        <tr>
          <td><strong>Min / Max Latency</strong></td>
          <td>${reportData.minDuration.toFixed(2)} ms / ${reportData.maxDuration.toFixed(2)} ms</td>
        </tr>
        <tr>
          <td><strong>Test Date/Time</strong></td>
          <td>${new Date().toLocaleString()}</td>
        </tr>
      </tbody>
    </table>
  </div>
</body>
</html>`;

  fs.writeFileSync(outputPath, htmlContent, 'utf8');
  console.log(`HTML report saved to: ${outputPath}`);
}

async function main() {
  try {
    const summaryPath = path.join(__dirname, 'summary.json');
    if (!fs.existsSync(summaryPath)) {
      console.error(`Error: summary.json not found at ${summaryPath}`);
      process.exit(1);
    }

    const data = JSON.parse(fs.readFileSync(summaryPath, 'utf8'));
    const metrics = data.metrics || {};

    const httpReqs = metrics.http_reqs || {};
    const httpReqDuration = metrics.http_req_duration || {};
    const httpReqFailed = metrics.http_req_failed || {};
    const checks = metrics.checks || {};

    // Retrieve values defensively
    const totalRequests = getMetricValue(httpReqs, 'count') || 0;
    const rps = getMetricValue(httpReqs, 'rate') || 0;

    const avgDuration = getMetricValue(httpReqDuration, 'avg') || 0;
    const minDuration = getMetricValue(httpReqDuration, 'min') || 0;
    const maxDuration = getMetricValue(httpReqDuration, 'max') || 0;
    const p95Duration = getMetricValue(httpReqDuration, 'p(95)') || 0;

    const failureRate = getMetricValue(httpReqFailed, 'value') !== null ? (getMetricValue(httpReqFailed, 'value') * 100) : 0;
    const checkRate = getMetricValue(checks, 'value') !== null ? (getMetricValue(checks, 'value') * 100) : 0;
    const failedRequests = Math.round(totalRequests * (failureRate / 100));

    const targetUrl = process.env.BACKEND_URL || 'http://localhost:5000';
    const reportData = {
      targetUrl,
      vus: 100,
      duration: '1m',
      totalRequests,
      failedRequests,
      rps,
      avgDuration,
      minDuration,
      maxDuration,
      p95Duration,
      failureRate,
      checkRate
    };

    // Ensure output directories exist
    const excelDir = path.join(__dirname, 'reports');
    if (!fs.existsSync(excelDir)) {
      fs.mkdirSync(excelDir, { recursive: true });
    }

    const excelPath = path.join(excelDir, 'load-test-report.xlsx');
    const htmlPath = path.join(excelDir, 'load-test-report.html');

    await generateExcelReport(reportData, excelPath);
    generateHtmlReport(reportData, htmlPath);

    // Save backups under historical folders
    const dateStr = new Date().toISOString().replace(/T/, '_').replace(/:/g, '-').split('.')[0];
    const historyDir = path.join(__dirname, 'reports/history', dateStr);
    if (!fs.existsSync(historyDir)) {
      fs.mkdirSync(historyDir, { recursive: true });
    }
    
    await generateExcelReport(reportData, path.join(historyDir, 'load-test-report.xlsx'));
    generateHtmlReport(reportData, path.join(historyDir, 'load-test-report.html'));

    console.log('[LOADTEST] Summary parsed and reports exported successfully.');
  } catch (err) {
    console.error('[LOADTEST] Error during summary parsing:', err);
    process.exit(1);
  }
}

main();
