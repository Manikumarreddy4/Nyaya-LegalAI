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

async function generateExcelReport(reportData) {
  const workbook = new ExcelJS.Workbook();
  const sheet = workbook.addWorksheet('Load Test Summary');

  // Set grid lines visible
  sheet.views = [{ showGridLines: true }];

  // Title block
  sheet.mergeCells('A1:D1');
  const titleCell = sheet.getCell('A1');
  titleCell.value = 'API Baseline & Load Testing Report';
  titleCell.font = { name: 'Segoe UI', size: 16, bold: true, color: { argb: 'FFFFFF' } };
  titleCell.fill = {
    type: 'pattern',
    pattern: 'solid',
    fgColor: { argb: '4F46E5' } // Indigo color
  };
  titleCell.alignment = { vertical: 'middle', horizontal: 'center' };
  sheet.getRow(1).height = 40;

  // Metadata block
  sheet.getCell('A3').value = 'Target URL:';
  sheet.getCell('B3').value = reportData.targetUrl;
  sheet.getCell('A4').value = 'Test Date/Time:';
  sheet.getCell('B4').value = new Date().toISOString();
  sheet.getCell('A5').value = 'Duration:';
  sheet.getCell('B5').value = '1 Minute';
  sheet.getCell('A6').value = 'Virtual Users:';
  sheet.getCell('B6').value = '100 VUs';

  // Apply bold styling to metadata labels
  ['A3', 'A4', 'A5', 'A6'].forEach(cellRef => {
    sheet.getCell(cellRef).font = { name: 'Segoe UI', bold: true, size: 11 };
  });

  // Table Headers
  const headerRow = sheet.getRow(8);
  headerRow.values = ['Metric Name', 'Measured Value', 'Target Threshold', 'Status'];
  headerRow.height = 28;
  headerRow.eachCell(cell => {
    cell.font = { name: 'Segoe UI', bold: true, size: 11, color: { argb: 'FFFFFF' } };
    cell.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: '1E293B' } // Dark Slate
    };
    cell.alignment = { vertical: 'middle', horizontal: 'left' };
    cell.border = {
      top: { style: 'thin' },
      left: { style: 'thin' },
      bottom: { style: 'thin' },
      right: { style: 'thin' }
    };
  });

  // Data rows
  const rows = [
    {
      name: 'Total Requests Sent',
      val: `${reportData.totalRequests} requests`,
      thresh: 'N/A',
      status: '-'
    },
    {
      name: 'Throughput (RPS)',
      val: `${reportData.rps.toFixed(2)} req/sec`,
      thresh: 'N/A',
      status: '-'
    },
    {
      name: 'Average Response Time',
      val: `${reportData.avgDuration.toFixed(2)} ms`,
      thresh: 'N/A',
      status: '-'
    },
    {
      name: 'Min Response Time',
      val: `${reportData.minDuration.toFixed(2)} ms`,
      thresh: 'N/A',
      status: '-'
    },
    {
      name: 'Max Response Time',
      val: `${reportData.maxDuration.toFixed(2)} ms`,
      thresh: 'N/A',
      status: '-'
    },
    {
      name: '95th-Percentile Latency (p95)',
      val: `${reportData.p95Duration.toFixed(2)} ms`,
      thresh: '< 1500.00 ms',
      status: reportData.p95Duration < 1500 ? 'Pass' : 'Fail'
    },
    {
      name: 'Request Failure Rate',
      val: `${reportData.failureRate.toFixed(2)}%`,
      thresh: '< 5.00%',
      status: reportData.failureRate < 5 ? 'Pass' : 'Fail'
    },
    {
      name: 'Assertion Checks Pass Rate',
      val: `${reportData.checkRate.toFixed(2)}%`,
      thresh: '100.00%',
      status: reportData.checkRate === 100 ? 'Pass' : 'Warning'
    }
  ];

  rows.forEach((r, index) => {
    const rowNum = 9 + index;
    const currRow = sheet.getRow(rowNum);
    currRow.values = [r.name, r.val, r.thresh, r.status];
    currRow.height = 22;

    currRow.eachCell((cell, colIndex) => {
      cell.font = { name: 'Segoe UI', size: 10 };
      cell.border = {
        top: { style: 'thin', color: { argb: 'E2E8F0' } },
        left: { style: 'thin', color: { argb: 'E2E8F0' } },
        bottom: { style: 'thin', color: { argb: 'E2E8F0' } },
        right: { style: 'thin', color: { argb: 'E2E8F0' } }
      };
      
      // Highlight status cell
      if (colIndex === 4) {
        if (cell.value === 'Pass') {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'DCFCE7' } }; // Light Green
          cell.font = { name: 'Segoe UI', size: 10, bold: true, color: { argb: '166534' } };
        } else if (cell.value === 'Fail') {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FEE2E2' } }; // Light Red
          cell.font = { name: 'Segoe UI', size: 10, bold: true, color: { argb: '991B1B' } };
        } else if (cell.value === 'Warning') {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FEF3C7' } }; // Light Yellow
          cell.font = { name: 'Segoe UI', size: 10, bold: true, color: { argb: '92400E' } };
        }
      }
    });
  });

  // Adjust column widths to fit content
  sheet.columns.forEach(column => {
    let maxLen = 0;
    column.eachCell({ includeEmpty: true }, cell => {
      const valStr = cell.value ? cell.value.toString() : '';
      if (valStr.length > maxLen) {
        maxLen = valStr.length;
      }
    });
    column.width = Math.max(maxLen + 4, 15);
  });

  const outputPath = path.join(__dirname, '../load-test-report.xlsx');
  await workbook.xlsx.writeFile(outputPath);
  console.log(`Successfully generated Excel report at ${outputPath}`);
}

async function run() {
  try {
    const summaryPath = path.join(__dirname, '../summary.json');
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

    // Extract metrics safely
    const totalRequests = getMetricValue(httpReqs, 'count') || 0;
    const rps = getMetricValue(httpReqs, 'rate') || 0;

    const avgDuration = getMetricValue(httpReqDuration, 'avg') || 0;
    const minDuration = getMetricValue(httpReqDuration, 'min') || 0;
    const maxDuration = getMetricValue(httpReqDuration, 'max') || 0;
    const p95Duration = getMetricValue(httpReqDuration, 'p(95)') || 0;

    const failureRate = getMetricValue(httpReqFailed, 'value') !== null ? (getMetricValue(httpReqFailed, 'value') * 100) : 0;
    const checkRate = getMetricValue(checks, 'value') !== null ? (getMetricValue(checks, 'value') * 100) : 0;

    const mdReport = `
### 📈 k6 API Baseline & Load Testing Executive Summary

| Metric | Measured Value | Target Threshold | Status |
| :--- | :--- | :--- | :--- |
| **Total Requests Sent** | ${totalRequests} requests | N/A | - |
| **Throughput (RPS)** | ${rps.toFixed(2)} req/sec | N/A | - |
| **Average Response Time** | ${avgDuration.toFixed(2)} ms | N/A | - |
| **Min Response Time** | ${minDuration.toFixed(2)} ms | N/A | - |
| **Max Response Time** | ${maxDuration.toFixed(2)} ms | N/A | - |
| **95th-Percentile Latency (p95)** | ${p95Duration.toFixed(2)} ms | < 1500.00 ms | ${p95Duration < 1500 ? '✅ Pass' : '❌ Fail'} |
| **Request Failure Rate** | ${failureRate.toFixed(2)}% | < 5.00% | ${failureRate < 5 ? '✅ Pass' : '❌ Fail'} |
| **Assertion Checks Pass Rate** | ${checkRate.toFixed(2)}% | 100% | ${checkRate === 100 ? '✅ Pass' : '⚠️ Warning'} |
`;

    console.log(mdReport);

    const stepSummaryPath = process.env.GITHUB_STEP_SUMMARY;
    if (stepSummaryPath) {
      fs.appendFileSync(stepSummaryPath, mdReport);
      console.log(`Successfully appended report to ${stepSummaryPath}`);
    } else {
      console.log("No GITHUB_STEP_SUMMARY path found, printing to console instead.");
    }

    // Generate Excel report
    const targetUrl = process.env.BACKEND_URL || 'http://localhost:5000';
    await generateExcelReport({
      targetUrl,
      totalRequests,
      rps,
      avgDuration,
      minDuration,
      maxDuration,
      p95Duration,
      failureRate,
      checkRate
    });

  } catch (error) {
    console.error("Error parsing k6 summary: ", error);
    process.exit(1);
  }
}

run();
