# Appium Mobile E2E Automation Framework

This framework provides End-to-End mobile UI validation for the **Nyaya Legal AI** Android application using **Appium 2.x**, **WebdriverIO**, **Mocha**, **Chai**, **Winston**, and **ExcelJS**.

---

## 🛠️ Technology Stack & Architecture
* **Core Runner**: Node.js + ES6 Modules
* **WebDriver Client**: WebdriverIO v8
* **Test Specs**: Mocha + Chai assertions
* **Architecture**: Page Object Model (POM)
* **Reporting**: Mochawesome + ExcelJS (`React native_E2E_Report.xlsx`) + Dark-themed HTML Dashboard
* **Logging**: Winston Logger

---

## 📂 Directory Structure
```text
AppiumTesting/
├── config/
│   └── appium.config.js       # Appium driver capabilities and port mapping
├── driver/
│   └── driverFactory.js      # Appium session builder & custom RN elements finders
├── pages/
│   ├── BasePage.js           # Common waits, alerts, and screenshots failures capture
│   ├── LoginPage.js          # Authentication elements POM
│   └── DashboardPage.js      # Consultations list, forms, and filters POM
├── utils/
│   ├── excelReporter.js      # Compiles React native_E2E_Report.xlsx (4 sheets)
│   ├── htmlReporter.js       # Renders reports/index.html dashboard
│   ├── logger.js             # Winston logger configuration
│   └── smartAiTester.js      # AI-assisted screen analyzer widget discovery
├── tests/
│   ├── runner.js             # Unified orchestrator executing specs and mapping results
│   ├── test-definitions.js  # 1200 E2E functional test cases definition registry
│   └── e2e-tests.spec.js     # Mocha specifications verifying application workflows
├── mocha.config.cjs          # Mocha timeout and Mochawesome configurations
└── package.json              # Script launcher and packages configurations
```

---

## 🚀 Setup & Execution

### 1. Prerequisites
* Install [Node.js](https://nodejs.org/) (v18+)
* Install Android Studio, Android SDK, and ensure `ANDROID_HOME` environment variable is configured.
* Set up an Android Emulator or connect a physical device via USB Debugging.
* Install Appium globally:
  ```bash
  npm install -g appium
  ```
* Install UiAutomator2 Driver:
  ```bash
  appium driver install uiautomator2
  ```

### 2. Install Dependencies
Navigate to the directory and run:
```bash
npm install
```

### 3. Execution
Start your Appium Server:
```bash
appium
```

Run the automated test suite:
```bash
npm run test
```

### 4. Failure Recovery & Logs
* Logs are stored under `logs/appium.log`.
* Screen state dumps, XML widget tree structure, and failure screenshots are recorded in `reports/failures/` on assertion failure.
* The Excel spreadsheet is exported to `reports/excel/React native_E2E_Report.xlsx`.
* The HTML report is compiled at `reports/index.html`.
