import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { logger } from '../utils/logger.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export class BasePage {
  constructor(client) {
    this.client = client;
  }

  async findEl(selector) {
    if (!this.client) {
      logger.info(`[BasePage] [MOCK] Finding element by selector: ${selector}`);
      return { selector, mock: true };
    }
    return await this.client.$(selector);
  }

  async click(selector) {
    logger.info(`[BasePage] Clicking element: ${selector}`);
    if (!this.client) return;
    const el = await this.findEl(selector);
    await el.waitForDisplayed({ timeout: 10000 });
    await el.click();
  }

  async type(selector, value) {
    logger.info(`[BasePage] Typing "${value}" into: ${selector}`);
    if (!this.client) return;
    const el = await this.findEl(selector);
    await el.waitForDisplayed({ timeout: 10000 });
    await el.setValue(value);
  }

  async waitForDisplayed(selector, timeout = 10000) {
    logger.info(`[BasePage] Waiting for element: ${selector}`);
    if (!this.client) return true;
    const el = await this.findEl(selector);
    return await el.waitForDisplayed({ timeout });
  }

  async captureFailure(testName, error = '') {
    const timestamp = new Date().toISOString().replace(/:/g, '-').replace(/\..+/, '');
    const safeName = testName.replace(/[^a-z0-9]/gi, '_').toLowerCase();
    const folder = path.join(__dirname, '../reports/failures');
    if (!fs.existsSync(folder)) {
      fs.mkdirSync(folder, { recursive: true });
    }

    const screenshotPath = path.join(folder, `${safeName}_${timestamp}.png`);
    const logPath = path.join(folder, `${safeName}_${timestamp}.log`);
    const treePath = path.join(folder, `${safeName}_${timestamp}_tree.xml`);

    logger.error(`[BasePage] [FAILURE] Test '${testName}' failed! Capturing state under reports/failures/`);

    if (this.client) {
      try {
        // Capture screenshot
        await this.client.saveScreenshot(screenshotPath);
        logger.info(`[BasePage] Screenshot saved to ${screenshotPath}`);

        // Capture device logs
        const logs = await this.client.getLogs('logcat');
        fs.writeFileSync(logPath, JSON.stringify(logs, null, 2), 'utf8');
        logger.info(`[BasePage] Logcat logs saved to ${logPath}`);

        // Capture widget tree
        const source = await this.client.getPageSource();
        fs.writeFileSync(treePath, source, 'utf8');
        logger.info(`[BasePage] Widget tree saved to ${treePath}`);
      } catch (e) {
        logger.error(`[BasePage] Failed to capture live device failure state: ${e.message}`);
      }
    } else {
      // Mock failure captures
      fs.writeFileSync(screenshotPath, 'MOCK SCREENSHOT DATA', 'utf8');
      fs.writeFileSync(logPath, `MOCK DEVICE LOGS\nStack trace: ${error.stack || error}`, 'utf8');
      fs.writeFileSync(treePath, '<mock-widget-tree><widget type="TextField" id="input_phone" /></mock-widget-tree>', 'utf8');
      logger.info(`[BasePage] [MOCK] Simulating failure exports for ${testName}`);
    }

    return {
      screenshotPath,
      logPath,
      treePath
    };
  }
}

export default BasePage;
