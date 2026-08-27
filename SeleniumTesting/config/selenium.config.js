export const seleniumConfig = {
  baseUrl: process.env.TEST_BASE_URL || 'http://localhost:5173',
  browser: process.env.TEST_BROWSER || 'chrome', // chrome, firefox, edge
  headless: process.env.TEST_HEADLESS !== 'false', // true by default in CI
  explicitWaitMs: parseInt(process.env.EXPLICIT_WAIT_MS || '10000', 10),
  implicitWaitMs: parseInt(process.env.IMPLICIT_WAIT_MS || '2000', 10),
  windowWidth: 1280,
  windowHeight: 800
};

export default seleniumConfig;
