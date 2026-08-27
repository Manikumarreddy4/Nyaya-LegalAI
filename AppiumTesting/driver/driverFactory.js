import { remote } from 'webdriverio';
import appiumConfig from '../config/appium.config.js';
import { logger } from '../utils/logger.js';

export const find = {
  byValueKey: (key) => `//*[@resource-id="${key}" or @content-desc="${key}" or @text="${key}"]`,
  byText: (text) => `//*[@text="${text}" or contains(@text, "${text}")]`,
  bySemanticsLabel: (label) => `//*[@content-desc="${label}"]`,
  byAccessibilityId: (id) => `~${id}`
};

export class DriverFactory {
  static async createDriver() {
    logger.info('[DriverFactory] Attempting to initialize Appium session...');
    try {
      const client = await remote(appiumConfig);
      logger.info('[DriverFactory] Appium webdriverio session established successfully.');
      return client;
    } catch (err) {
      logger.warn(`[DriverFactory] Failed to connect to Appium Server at ${appiumConfig.hostname}:${appiumConfig.port}.`);
      logger.warn(`[DriverFactory] Error details: ${err.message}. Fallback to simulated execution.`);
      return null;
    }
  }
}

export default DriverFactory;
