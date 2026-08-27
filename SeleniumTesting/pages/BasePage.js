import { By, until } from 'selenium-webdriver';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { logger } from '../utilities/logger.js';
import seleniumConfig from '../config/selenium.config.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export class BasePage {
  constructor(driver) {
    this.driver = driver;
  }

  async getUrl(url) {
    logger.info(`[BasePage] Navigating to URL: ${url}`);
    if (!this.driver) return;
    await this.driver.get(url);
  }

  async findEl(selector, timeout = seleniumConfig.explicitWaitMs) {
    if (!this.driver) {
      logger.info(`[BasePage] [MOCK] Finding element: ${JSON.stringify(selector)}`);
      return { selector, mock: true };
    }
    const locator = typeof selector === 'string' ? By.css(selector) : selector;
    await this.driver.wait(until.elementLocated(locator), timeout);
    const element = await this.driver.findElement(locator);
    await this.driver.wait(until.elementIsVisible(element), timeout);
    return element;
  }

  async click(selector) {
    logger.info(`[BasePage] Clicking on: ${JSON.stringify(selector)}`);
    if (!this.driver) return;
    const element = await this.findEl(selector);
    await element.click();
  }

  async type(selector, value) {
    logger.info(`[BasePage] Typing "${value}" into: ${JSON.stringify(selector)}`);
    if (!this.driver) return;
    const element = await this.findEl(selector);
    await element.clear();
    await element.sendKeys(value);
  }

  async executeScript(script, ...args) {
    if (!this.driver) return;
    return await this.driver.executeScript(script, ...args);
  }

  async scrollIntoView(selector) {
    logger.info(`[BasePage] Scrolling element into view: ${JSON.stringify(selector)}`);
    if (!this.driver) return;
    const element = await this.findEl(selector);
    await this.executeScript("arguments[0].scrollIntoView({ behavior: 'smooth', block: 'center' });", element);
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

    logger.error(`[BasePage] [FAILURE] Web Test '${testName}' failed! Capturing screenshots & logs...`);

    if (this.driver) {
      try {
        // Screenshot
        const screenshotData = await this.driver.takeScreenshot();
        fs.writeFileSync(screenshotPath, screenshotData, 'base64');
        logger.info(`[BasePage] Screenshot saved to ${screenshotPath}`);

        // Browser logs
        let logs = [];
        try {
          logs = await this.driver.manage().logs().get('browser');
        } catch (e) {
          // Some drivers/browsers don't support log retrieval
        }
        const currentUrl = await this.driver.getCurrentUrl();
        const logsPayload = {
          currentUrl,
          browserLogs: logs,
          errorStack: error.stack || error
        };
        fs.writeFileSync(logPath, JSON.stringify(logsPayload, null, 2), 'utf8');
        logger.info(`[BasePage] Console logs and failure context saved to ${logPath}`);
      } catch (e) {
        logger.error(`[BasePage] Failed to capture live browser failures: ${e.message}`);
      }
    } else {
      // Mock failure files
      fs.writeFileSync(screenshotPath, 'MOCK SCREENSHOT DATA', 'utf8');
      fs.writeFileSync(logPath, `MOCK BROWSER CONSOLE LOGS\nStack: ${error.stack || error}`, 'utf8');
      logger.info(`[BasePage] [MOCK] Mock failure files exported for ${testName}`);
    }

    return { screenshotPath, logPath };
  }
}

export default BasePage;
