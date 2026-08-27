import { until } from 'selenium-webdriver';
import { logger } from './logger.js';

export const seleniumUtils = {
  async waitForElementText(driver, locator, expectedText, timeout = 10000) {
    logger.info(`[Utils] Waiting for element text: "${expectedText}"`);
    if (!driver) return true;
    const el = await driver.findElement(locator);
    return await driver.wait(until.elementTextContains(el, expectedText), timeout);
  },

  async handleAlert(driver, accept = true) {
    logger.info(`[Utils] Handling alert banner: ${accept ? 'Accept' : 'Dismiss'}`);
    if (!driver) return;
    try {
      const alert = await driver.switchTo().alert();
      const text = await alert.getText();
      logger.info(`[Utils] Alert text detected: "${text}"`);
      if (accept) {
        await alert.accept();
      } else {
        await alert.dismiss();
      }
      return text;
    } catch (err) {
      logger.warn(`[Utils] No alert popup active: ${err.message}`);
      return null;
    }
  },

  async retryAction(actionFn, retries = 3, delayMs = 1000) {
    logger.info(`[Utils] Executing action with retry safety (${retries} attempts)`);
    for (let attempt = 1; attempt <= retries; attempt++) {
      try {
        return await actionFn();
      } catch (err) {
        logger.warn(`[Utils] Attempt ${attempt} failed: ${err.message}. Retrying...`);
        if (attempt === retries) throw err;
        await new Promise(resolve => setTimeout(resolve, delayMs));
      }
    }
  },

  async manageSessionPersistence(driver, save = true) {
    logger.info(`[Utils] Managing storage state: ${save ? 'Save' : 'Clear'}`);
    if (!driver) return;
    if (save) {
      // Save local storage
      const token = await driver.executeScript("return localStorage.getItem('token');");
      logger.info(`[Utils] Saved auth token: ${token ? 'Active' : 'Empty'}`);
      return token;
    } else {
      await driver.executeScript("localStorage.clear(); sessionStorage.clear();");
      logger.info('[Utils] Storage data cleared out successfully.');
    }
  }
};

export default seleniumUtils;
