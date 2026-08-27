# Nyaya-LegalAI Project — Master Prompts & Recreation Guide

This guide contains the exact, detailed prompts, baseline specifications, and execution steps needed to recreate the E2E testing, security audit, and load testing configurations we built for the **Nyaya-LegalAI** application. 

By applying these prompts sequentially to a new AI assistant session in the repository, you will get all jobs and 400+ E2E test cases per domain passing, with structured Excel and HTML reports generated and deployed automatically to GitHub Pages.

---

## 🛠️ Phase 0: Workspace Directory Prep
Ensure the repository contains the following folder structure before running the prompts:
- [webapp](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/webapp) — The React/Vite web application frontend and Express server.
- [app](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/app) — The Android native codebase.
- [SeleniumTesting](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/SeleniumTesting) — The Web E2E Selenium tests folder.
- [AppiumTesting](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/AppiumTesting) — The Mobile automation Appium tests folder.
- [LoadTesting](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/LoadTesting) — The API Load testing k6 folder.
- [PythonTesting](file:///c:/Users/manik/AndroidStudioProjects/LegalAI/PythonTesting) — The Python testing and reports aggregator folder.

---

## 🌐 Prompt 1: Web Frontend E2E (400+ Selenium Tests) & Pages Deployment

### Objective
Generate 400+ unique assertions grouped across functional categories, run tests under headless Chrome, save Excel + HTML reports, and natively deploy the unified bundle (App + Reports) to GitHub Pages.

### Copy & Paste Master Prompt
```markdown
Please configure and integrate the Web E2E testing and GitHub Pages deployment pipelines for the Nyaya-LegalAI React/Vite project:

1. Web E2E Test Suite (400+ assertions):
- Utilize the `SeleniumTesting` directory to manage E2E tests.
- Define a comprehensive set of test cases covering user authentication, navigation, chat history, booking slots, and accessibility options (totaling 400+ assertions).
- Implement hooks that initialize a ChromeDriver session under headless execution. Cleanly target the configured BASE_URL.

2. Mocha Excel & HTML Reporters:
- Configure Excel reporting using the `exceljs` library.
  - Listen to test pass/fail events. 
  - Log test results, timestamps, and execution status.
  - Automatically write results to `SeleniumTesting/reports/excel/E2E_Report.xlsx`.
- Implement HTML rendering to generate a dark-themed, responsive HTML report (`SeleniumTesting/reports/index.html`) containing total stats, summary charts, and detailed stack trace info on failures.

3. CI Background Server Initialization:
- In the CI/CD pipeline, launch the Express server and Vite frontend server in the background:
  `cd webapp && node server.js & npm run dev &`
- Add a startup buffer and curl-based check to verify both endpoints are responsive before running the Selenium test runner.

4. GitHub Pages Deployment:
- Copy the generated reports into the web app build distribution directory `webapp/dist/`.
- Deploy the unified build output directly to GitHub Pages using:
  - `actions/upload-pages-artifact@v3` with path `webapp/dist`
  - `actions/deploy-pages@v4`
```

---

## 🛡️ Prompt 2: DAST Security Audit Pipeline (OWASP Top 10)

### Objective
Create an automated security scanner evaluating HTTP headers, public routes, authentication validation, input sanitization, and invalid route handling. Generate Excel sheets and Markdown summaries detailing findings, and enforce a Zero-Critical gate.

### Copy & Paste Master Prompt
```markdown
Please implement the DAST Security Pipeline and OWASP Top 10 scanning scripts:

1. DAST Security Suite:
- Configure automated probers to audit security policies:
  - Check for essential HTTP security headers (CORS headers, X-Frame-Options, X-Content-Type-Options, etc.).
  - Check safe public endpoints and verify access control.
  - Query invalid paths to ensure input sanitization and verification of error responses (preventing unhandled 500 server crashes).
  - Verify authorization barriers on private endpoints (returning 401 Unauthorized / 403 Forbidden).
- Write a security scanning script under `PythonTesting/` that executes these scans.
- Report all findings in two formats: a styled Excel workbook (`findings.xlsx` containing Security Findings, Vulnerabilities, and Risk Summary sheets) and a Markdown report (`security-review.md`).

2. CI Integration and Gatekeeper:
- Integrate the DAST security audit into the `dast-security-tests` job in `.github/workflows/enterprise-ci-cd.yml`.
- Execute the script, append the executive summaries directly to the GHA step summary, and enforce a Zero-Critical security policy: fail the GHA run immediately if critical vulnerabilities are discovered.
```

---

## 📱 Prompt 3: Mobile Appium E2E (400+ Android Tests) & CI Emulator Runner

### Objective
Create parameterized Appium tests inside the Android emulator on `ubuntu-latest`. Set up the Android SDK, Java, and Node dependencies, launch the emulator using KVM, run the test suites, and upload reports.

### Copy & Paste Master Prompt
```markdown
Please configure and integrate the Mobile E2E Appium testing pipeline for the Android native application:

1. Parameterized Appium Spec (400+ assertions):
- Configure Appium automation in the `AppiumTesting/` directory.
- Define test cases covering Authentication, Registration, Navigation, and Compose components.
- Establish an Appium server connection, load the compiled Android debug APK, and automate UI interactions.

2. CI Emulator Runner (ubuntu-latest):
- Configure the Appium runner job (`appium-mobile-tests`) to run on `ubuntu-latest`.
- Set up Java JDK 17 and Android SDK platforms using `android-actions/setup-android@v3`.
- Set up Node.js and install Appium and UIAutomator2 drivers.
- Build the Android debug APK using `./gradlew assembleDebug --no-daemon` with proper Gradle memory restrictions (`GRADLE_OPTS: -Dorg.gradle.jvmargs="-Xmx2048m -XX:MaxMetaspaceSize=512m"`).
- Launch the emulator using `reactivecircus/android-emulator-runner@v2` targeting API Level 30.
- Execute Appium tests, export reports to `AppiumTesting/reports/`, and save JSON logs for reports bundling.
```

---

## 📈 Prompt 4: API Baseline / Load Testing (k6 & Summary Parser)

### Objective
Configure a standalone load testing runner evaluating system capabilities under normal expected concurrent users. Track throughput (RPS) and latency (Average, Min, Max, p95) using k6, and parse outputs into worksheets.

### Baseline Specifications
* **Concurrent Users**: 100 virtual users (VUs)
* **Duration**: Running continuously for 1 minute
* **Throughput Target**: High throughput (e.g. 100+ requests/sec)
* **Latency Limits**: Fast response times (Average under 250ms, Min under 50ms, Max under 1500ms)

### Copy & Paste Master Prompt
```markdown
Please configure a baseline API load testing pipeline for the Nyaya-LegalAI Express server:

1. k6 Performance Configuration:
- Create `LoadTesting/load-test.js` to execute load testing using `k6`.
- Define options for 100 Virtual Users (`vus: 100`) running for a duration of 1 minute (`duration: '1m'`).
- Enforce metric thresholds: request failures under 5% (`rate<0.05`) and 95th-percentile latencies under 1.5 seconds (`p(95)<1500`).
- Target the server URL from environment variables (`__ENV.BACKEND_URL` or fallback `http://localhost:5000`) and make HTTP GET/POST queries.

2. Summary JSON Parser:
- Implement `LoadTesting/parseK6Summary.js` using the `exceljs` library.
- Read `summary.json` generated by k6, extract:
  - Throughput (RPS - Requests per Second)
  - Average, Minimum, Maximum, and p95 latency
  - Success and failure rates
- Generate an Excel workbook (`LoadTesting/reports/load-test-report.xlsx`) and an HTML dashboard report (`LoadTesting/reports/load-test-report.html`).
- Append the results as a markdown table directly to the GHA step summary.
```

---

## 💡 Key Lessons Learned & Implementation Hardening

When reproducing these pipelines, ensure the following critical issues are avoided:

1. **Gradle Memory Limits on Linux**: Standard Linux runners on GitHub Actions only have 7 GB of RAM. Running `./gradlew assembleDebug` without memory limits will result in random compiler crashes (OOM). Always use `--no-daemon` and set:
   `GRADLE_OPTS: -Dorg.gradle.jvmargs="-Xmx2048m -XX:MaxMetaspaceSize=512m"`
2. **Environment Path Overrides**: Do not override `ANDROID_HOME` or `ANDROID_SDK_ROOT` with empty workflow context properties in Gradle steps. Let the runner resolve them naturally from the setup action.
3. **No-concurrency Cancellation**: To prevent GHA from cancelling waiting or active test runs when a new push is made, remove the `concurrency` block entirely or set `cancel-in-progress: false` with unique run IDs.
4. **Combined Reporting**: Merging results from multiple parallel jobs requires individual runs to upload their result files (like JSON test metrics) as separate artifacts, which are then downloaded, compiled, and bundled during the downstream `bundle-reports` job.
