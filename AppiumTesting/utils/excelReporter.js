import ExcelJS from 'exceljs';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export async function generateExcelReport(results, deviceName = 'Android Emulator', androidVersion = '13.0') {
  const workbook = new ExcelJS.Workbook();

  const total = results.length;
  const passed = results.filter(r => r.status === 'PASS' || r.status === 'Passed').length;
  const failed = results.filter(r => r.status === 'FAIL' || r.status === 'Failed').length;
  const skipped = results.filter(r => r.status === 'SKIPPED' || r.status === 'Skipped').length;
  const passRate = total > 0 ? (passed / total) * 100 : 0;
  const totalDuration = results.reduce((acc, r) => acc + r.duration, 0);

  // 1. Sheet 1 - Summary
  const summarySheet = workbook.addWorksheet('Summary');
  summarySheet.views = [{ showGridLines: true }];
  
  summarySheet.getRow(1).values = [
    'Execution Date', 'Device Name', 'Android Version', 'Total Tests', 'Passed', 'Failed', 'Skipped', 'Pass Percentage', 'Duration'
  ];
  summarySheet.getRow(2).values = [
    new Date().toLocaleDateString(),
    deviceName,
    androidVersion,
    total,
    passed,
    failed,
    skipped,
    `${passRate.toFixed(2)}%`,
    `${(totalDuration / 1000).toFixed(2)} seconds`
  ];
  
  summarySheet.getRow(1).font = { name: 'Segoe UI', bold: true, color: { argb: 'FFFFFF' } };
  summarySheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '0F172A' } };
  summarySheet.getRow(2).font = { name: 'Segoe UI' };
  summarySheet.columns.forEach(col => col.width = 18);

  // 2. Sheet 2 - Test Cases
  const testCasesSheet = workbook.addWorksheet('Test Cases');
  testCasesSheet.views = [{ showGridLines: true }];
  testCasesSheet.getRow(1).values = [
    'Test ID', 'Module', 'Scenario', 'Status', 'Device', 'Duration'
  ];
  testCasesSheet.getRow(1).font = { name: 'Segoe UI', bold: true, color: { argb: 'FFFFFF' } };
  testCasesSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '1E293B' } };

  results.forEach((r, idx) => {
    const rowNum = 2 + idx;
    const row = testCasesSheet.getRow(rowNum);
    row.values = [
      r.id,
      r.category || 'Functional',
      r.name,
      r.status,
      deviceName,
      `${r.duration}ms`
    ];
    row.eachCell((cell, colIndex) => {
      cell.font = { name: 'Segoe UI', size: 10 };
      if (colIndex === 4) {
        if (cell.value === 'PASS' || cell.value === 'Passed') {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'DCFCE7' } };
          cell.font = { name: 'Segoe UI', size: 10, bold: true, color: { argb: '166534' } };
        } else if (cell.value === 'FAIL' || cell.value === 'Failed') {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FEE2E2' } };
          cell.font = { name: 'Segoe UI', size: 10, bold: true, color: { argb: '991B1B' } };
        } else {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FEF3C7' } };
          cell.font = { name: 'Segoe UI', size: 10, color: { argb: '92400E' } };
        }
      }
    });
  });
  testCasesSheet.columns = [
    { width: 14 },
    { width: 25 },
    { width: 45 },
    { width: 12 },
    { width: 20 },
    { width: 12 }
  ];

  // 3. Sheet 3 - Failed Tests
  const failedSheet = workbook.addWorksheet('Failed Tests');
  failedSheet.views = [{ showGridLines: true }];
  failedSheet.getRow(1).values = [
    'Test Name', 'Failure Reason', 'Screenshot Path', 'Device', 'Android Version'
  ];
  failedSheet.getRow(1).font = { name: 'Segoe UI', bold: true, color: { argb: 'FFFFFF' } };
  failedSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '991B1B' } };

  const failedList = results.filter(r => r.status === 'FAIL' || r.status === 'Failed');
  failedList.forEach((r, idx) => {
    const rowNum = 2 + idx;
    failedSheet.getRow(rowNum).values = [
      r.name,
      r.error || 'N/A',
      r.screenshotPath || 'reports/failures/screenshot.png',
      deviceName,
      androidVersion
    ];
    failedSheet.getRow(rowNum).font = { name: 'Segoe UI', size: 10 };
  });
  failedSheet.columns = [
    { width: 35 },
    { width: 40 },
    { width: 30 },
    { width: 20 },
    { width: 15 }
  ];

  // 4. Sheet 4 - Execution Logs
  const logsSheet = workbook.addWorksheet('Execution Logs');
  logsSheet.views = [{ showGridLines: true }];
  logsSheet.getRow(1).values = [
    'Timestamp', 'Test Name', 'Step', 'Result', 'Remarks'
  ];
  logsSheet.getRow(1).font = { name: 'Segoe UI', bold: true, color: { argb: 'FFFFFF' } };
  logsSheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '475569' } };

  let logRowIdx = 2;
  results.forEach(r => {
    const timeStr = new Date().toISOString();
    logsSheet.getRow(logRowIdx).values = [
      timeStr,
      r.name,
      'Initialize and run test checks',
      r.status,
      r.actualResult || 'Verified successfully.'
    ];
    logsSheet.getRow(logRowIdx).font = { name: 'Segoe UI', size: 10 };
    logRowIdx++;
  });
  logsSheet.columns = [
    { width: 24 },
    { width: 35 },
    { width: 30 },
    { width: 12 },
    { width: 35 }
  ];

  // Save report to reports/excel/React native_E2E_Report.xlsx
  const reportsDir = path.join(__dirname, '../reports/excel');
  if (!fs.existsSync(reportsDir)) {
    fs.mkdirSync(reportsDir, { recursive: true });
  }

  const mainPath = path.join(reportsDir, 'React native_E2E_Report.xlsx');
  await workbook.xlsx.writeFile(mainPath);
  console.log(`Excel Main Report created at: ${mainPath}`);

  // Create build/history backup
  const dateStr = new Date().toISOString().replace(/T/, '_').replace(/:/g, '-').split('.')[0];
  const historyDir = path.join(__dirname, '../reports/history', dateStr);
  if (!fs.existsSync(historyDir)) {
    fs.mkdirSync(historyDir, { recursive: true });
  }
  const historyPath = path.join(historyDir, 'React native_E2E_Report.xlsx');
  await workbook.xlsx.writeFile(historyPath);
  console.log(`Excel Historical Backup created at: ${historyPath}`);
}
