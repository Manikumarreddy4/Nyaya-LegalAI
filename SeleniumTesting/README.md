# Selenium Web E2E Automation Framework

This framework provides End-to-End browser UI validation for the **Nyaya Legal AI** React application using **Selenium WebDriver**, **Mocha**, **Chai**, **Winston**, and **ExcelJS**.

---

## 🛠️ Technology Stack & Architecture
* **Core Runner**: Node.js + ES6 Modules
* **WebDriver Client**: Selenium WebDriver v4
* **Test Specs**: Mocha + Chai assertions
* **Architecture**: Page Object Model (POM)
* **Reporting**: Mochawesome + ExcelJS (`E2E_Report.xlsx`) + HTML Dashboard
* **Logging**: Winston Logger
* **Smart Capabilities**: Dynamic forms and React routes validation rule scanner

---

## 📂 Directory Structure
```text
SeleniumTesting/
├── config/
│   └── selenium.config.js    # Browser settings, headless toggles, and URL mappings
├── pages/
│   ├── BasePage.js           # Explicit waits, alerts, and browser screenshot helper
│   ├── LoginPage.js          # Authentication inputs and validations POM
│   └── DashboardPage.js      # Bookings schedule forms, sidebar, and headers POM
├── utilities/
│   ├── excelReporter.js      # Generates detailed E2E_Report.xlsx (4 sheets)
│   ├── htmlReporter.js       # Renders reports/index.html dashboard
│   ├── logger.js             # Winston logger configuration
│   ├── seleniumUtils.js      # Cookie saving, alerts switching, and retry mechanisms
│   └── dynamicTestGenerator.js # Smart React routes & validations test generator
├── tests/
│   ├── runner.js             # Runs specs, compiles metrics, and outputs dashboards
│   ├── test-definitions.js  # 1200 Web E2E test case registry
│   └── e2e-tests.spec.js     # BDD E2E specifications
├── mocha.config.cjs          # Mocha timeout and reporter options
└── package.json              # Script launcher and packages configurations
```

---

## 🚀 Setup & Execution

### 1. Prerequisites
* Install [Node.js](https://nodejs.org/) (v18+)
* Install web browsers (Google Chrome, Microsoft Edge, or Firefox).
* Installs matching WebDrivers automatically (handled by selenium-webdriver manager at runtime).

### 2. Install Dependencies
Navigate to the directory and run:
```bash
npm install
```

### 3. Execution
Start your local dev server in `webapp/` first:
```bash
cd webapp
npm run dev
```

Run the web test suite:
```bash
npm run test
```

### 4. Failure Recovery & Logs
* Execution logs are captured under `logs/selenium.log`.
* On assertion failure, logs, stack traces, console outputs, and base64 screenshots are written to `reports/failures/`.
* The spreadsheet is exported to `reports/excel/E2E_Report.xlsx`.
* The HTML dashboard is generated at `reports/index.html`.
