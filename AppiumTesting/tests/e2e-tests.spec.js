import { expect } from 'chai';
import DriverFactory from '../driver/driverFactory.js';
import LoginPage from '../pages/LoginPage.js';
import DashboardPage from '../pages/DashboardPage.js';
import gestureUtils from '../utils/gestureUtils.js';
import { logger } from '../utils/logger.js';
import SmartAiTester from '../utils/smartAiTester.js';

describe('Nyaya Legal AI - Appium Mobile E2E Automation', function () {
  this.timeout(60000);
  let client;
  let loginPage;
  let dashboardPage;

  before(async function () {
    logger.info('Starting Mobile Appium E2E Automation spec run...');
    client = await DriverFactory.createDriver();
    loginPage = new LoginPage(client);
    dashboardPage = new DashboardPage(client);
  });

  after(async function () {
    if (client) {
      try {
        await client.deleteSession();
        logger.info('Appium webdriver session closed successfully.');
      } catch (err) {
        logger.error(`Error closing Appium session: ${err.message}`);
      }
    }
  });

  describe('1. Authentication Module', function () {
    it('should validate empty username & password fields', async function () {
      logger.info('[Test] Executing Authentication empty field validation');
      try {
        await loginPage.login('', '');
        const validation = await loginPage.getValidationText();
        expect(validation).to.contain('required');
      } catch (err) {
        await loginPage.captureFailure('Auth_Empty_Fields', err);
        throw err;
      }
    });

    it('should show error banner for invalid credentials login', async function () {
      logger.info('[Test] Executing invalid credentials login validation');
      try {
        await loginPage.login('invalid@example.com', 'WrongPass123!');
        const errorText = await loginPage.getValidationText();
        expect(errorText).to.be.a('string');
      } catch (err) {
        await loginPage.captureFailure('Auth_Invalid_Credentials', err);
        throw err;
      }
    });

    it('should successfully log in user with valid credentials', async function () {
      logger.info('[Test] Executing successful authentication login flow');
      try {
        await loginPage.login('client@example.com', 'ClientPass123!');
        const dashboardTitle = await dashboardPage.waitForDisplayed(dashboardPage.legalAssistantCard);
        expect(dashboardTitle).to.be.true;
      } catch (err) {
        await loginPage.captureFailure('Auth_Valid_Login', err);
        throw err;
      }
    });
  });

  describe('2. Form Validation Module', function () {
    it('should validate required phone number and format matches 10 digits', async function () {
      logger.info('[Test] Validate registration phone formats');
      try {
        // Toggle signup screen
        await loginPage.register('Test User', 'signup@example.com', '123', 'Pass123!');
        const validation = await loginPage.getValidationText();
        expect(validation).to.contain('10 digits');
      } catch (err) {
        await loginPage.captureFailure('Form_Phone_Validation', err);
        throw err;
      }
    });

    it('should validate password complexity criteria', async function () {
      logger.info('[Test] Validate password security complexity checks');
      try {
        await loginPage.register('Test User', 'signup@example.com', '9876543210', '123');
        const validation = await loginPage.getValidationText();
        expect(validation).to.contain('Password must contain');
      } catch (err) {
        await loginPage.captureFailure('Form_Password_Complexity', err);
        throw err;
      }
    });
  });

  describe('3. UI Component & Navigation Module', function () {
    it('should verify quick actions and dashboard widget presence', async function () {
      logger.info('[Test] Checking quick action buttons layout');
      const assistantCardVisible = await dashboardPage.waitForDisplayed(dashboardPage.legalAssistantCard);
      expect(assistantCardVisible).to.be.true;
    });

    it('should submit consultation booking form successfully', async function () {
      logger.info('[Test] Submitting consultation schedule request');
      try {
        await dashboardPage.bookConsultation('9999988888', 'Online', '2026-09-01', '14:30');
        const successVisible = await dashboardPage.waitForDisplayed(dashboardPage.bookingSuccessMsg);
        expect(successVisible).to.be.true;
      } catch (err) {
        await dashboardPage.captureFailure('Booking_Form_Submission', err);
        throw err;
      }
    });
  });

  describe('4. Mobile Gestures Module', function () {
    it('should perform Scroll & Tap gesture actions without exception', async function () {
      logger.info('[Test] Executing swipe and scroll gestures');
      try {
        await gestureUtils.scroll(client, 'down');
        const el = await dashboardPage.findEl(dashboardPage.encyclopediaCard);
        await gestureUtils.tap(client, el);
        expect(true).to.be.true;
      } catch (err) {
        await dashboardPage.captureFailure('Gesture_Scroll_Tap', err);
        throw err;
      }
    });
  });

  describe('5. Smart AI Assister Screen Analyzer', function () {
    it('should automatically discover screen widgets and validation scenarios', async function () {
      logger.info('[Test] Invoking Smart AI test scenarios generator');
      const aiTester = new SmartAiTester(client);
      const report = await aiTester.analyzeCurrentScreen();
      
      expect(report.widgets).to.be.an('array');
      expect(report.scenarios).to.be.an('array');
      expect(report.scenarios.length).to.be.greaterThan(0);
      logger.info(`[Test] AI Screen Analyzer generated ${report.scenarios.length} test configurations.`);
    });
  });
});
