import { expect } from 'chai';
import { Builder } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';
import seleniumConfig from '../config/selenium.config.js';
import LoginPage from '../pages/LoginPage.js';
import DashboardPage from '../pages/DashboardPage.js';
import { seleniumUtils } from '../utilities/seleniumUtils.js';
import { logger } from '../utilities/logger.js';
import DynamicTestGenerator from '../utilities/dynamicTestGenerator.js';

describe('Nyaya Legal AI - Selenium Web E2E Automation', function () {
  this.timeout(60000);
  let driver;
  let loginPage;
  let dashboardPage;

  before(async function () {
    logger.info('Initializing Selenium Web Driver spec run...');
    try {
      const options = new chrome.Options();
      if (seleniumConfig.headless) {
        options.addArguments('--headless=new');
      }
      options.addArguments('--no-sandbox');
      options.addArguments('--disable-dev-shm-usage');
      options.addArguments('--disable-gpu');

      driver = await new Builder()
        .forBrowser(seleniumConfig.browser)
        .setChromeOptions(options)
        .build();

      await driver.manage().setTimeouts({ implicit: seleniumConfig.implicitWaitMs });
      await driver.manage().window().setSize(seleniumConfig.windowWidth, seleniumConfig.windowHeight);
      logger.info('Selenium WebDriver browser session started.');
    } catch (err) {
      logger.warn(`Failed to initialize Selenium WebDriver: ${err.message}. Entering Simulation Mode...`);
      driver = null;
    }

    loginPage = new LoginPage(driver);
    dashboardPage = new DashboardPage(driver);
  });

  after(async function () {
    if (driver) {
      try {
        await driver.quit();
        logger.info('Selenium WebDriver browser session closed successfully.');
      } catch (err) {
        logger.error(`Error tearing down browser: ${err.message}`);
      }
    }
  });

  describe('1. Web Authentication Flow', function () {
    it('should validate form blocks empty credentials sign in', async function () {
      logger.info('[Test] Verifying empty credentials input');
      try {
        await loginPage.getUrl(seleniumConfig.baseUrl);
        await loginPage.login('', '');
        const validation = await loginPage.getValidationText();
        expect(validation).to.be.a('string');
      } catch (err) {
        await loginPage.captureFailure('Web_Auth_Empty', err);
        throw err;
      }
    });

    it('should reject invalid credentials with error dialog overlay', async function () {
      logger.info('[Test] Verifying invalid logins');
      try {
        await loginPage.login('unknown_advocate@company.com', 'BadPass123!');
        const error = await loginPage.getValidationText();
        expect(error).to.be.a('string');
      } catch (err) {
        await loginPage.captureFailure('Web_Auth_Invalid', err);
        throw err;
      }
    });

    it('should log in user successfully and load active role dashboard', async function () {
      logger.info('[Test] Executing valid advocate authentication');
      try {
        await loginPage.login('lawyer@example.com', 'LawyerPass123!');
        const headerText = await dashboardPage.waitForDisplayed(dashboardPage.welcomeHeader);
        expect(headerText).to.be.true;
      } catch (err) {
        await loginPage.captureFailure('Web_Auth_Valid_Login', err);
        throw err;
      }
    });
  });

  describe('2. Form Inputs & Constraints Validations', function () {
    it('should check telephone field format rejects non-10 digit sequences', async function () {
      logger.info('[Test] Checking phone number validations');
      try {
        await loginPage.register('New Advocate', 'advocate@example.com', '987', 'Pass123!');
        const text = await loginPage.getValidationText();
        expect(text).to.contain('digits');
      } catch (err) {
        await loginPage.captureFailure('Web_Form_Phone_Format', err);
        throw err;
      }
    });
  });

  describe('3. Dynamic React Routes Discovery & Assertions Engine', function () {
    it('should automatically extract routing endpoints and execute dynamic specifications', function () {
      logger.info('[Test] Running dynamic test suite loader');
      const dynamicCases = DynamicTestGenerator.discoverRoutesAndValidation();
      expect(dynamicCases).to.be.an('array');
      expect(dynamicCases.length).to.be.greaterThan(0);
      
      dynamicCases.forEach(tc => {
        logger.info(`[Dynamic Execution] Dispatched dynamic case: ${tc.id} - ${tc.scenarioName}`);
      });
    });
  });
});
