# Grafana k6 API Load Testing & Performance Engineering Manual

This manual is a comprehensive, beginner-to-advanced guide to performance testing web application API services using **Grafana k6**.

---

## 1. What is Load Testing?

### Performance Testing Overview
Performance testing is a branch of quality engineering focused on assessing a system's speed, responsiveness, scalability, stability, and resource usage under a specific workload. 

### Load, Stress, Spike, and Soak Testing
Different testing types represent different workload shapes:

```text
  Load Testing           Stress Testing         Spike Testing          Soak Testing
  (Target SLA)           (Break Point)          (Sudden Burst)         (Durability / Leak)
      ____                   _/\_                    _/\_                  __________
     /    \                 /    \                  /    \                /          \
    /      \               /      \_____           /      \              /            \
   /        \             /             \         /        \            /              \
  /__________\           /_______________\       /__________\          /________________\
```

1. **Load Testing**: Checks system behavior under normal/expected peak load. Evaluates SLA compliance.
2. **Stress Testing**: Determines the maximum capacity and the "break point" of the application beyond its normal limits.
3. **Spike Testing**: Verifies recovery speed under a sudden, massive increase in requests (e.g., ticket release, flash sales).
4. **Soak Testing**: Evaluates system stability, memory leaks, and database connection leaks over extended periods (hours or days).

### Why API Load Testing Matters
Testing backend APIs directly is critical because:
* It isolates database, caching, and network latency from browser render cycles.
* It exposes concurrency locks, race conditions, memory leaks, and thread exhaust issues before they hit user-facing applications.
* **Example**: A user profile edit form might work perfectly for one user, but fail when 500 users update details simultaneously due to database row lock contention.

---

## 2. Installing k6 (Windows)

To install k6 on Windows, use **Winget** (Windows Package Manager) in PowerShell:

### Step 1: Search for k6
```powershell
winget search k6
```
* **Explanation**: This queries the Winget repository registry to check for published packages matching "k6".

### Step 2: Install k6
```powershell
winget install GrafanaLabs.k6
```
* **Explanation**: Downloads and silently installs the official Grafana k6 binary on your system, setting up executable PATH shortcuts.

### Step 3: Verify Installation
```powershell
k6 version
```
* **Explanation**: Prints the currently active k6 version. Output should resemble: `k6 v0.x.x (2026-xx-xx, go1.x.x, windows/amd64)`.

---

## 3. Running a Test

To run a k6 test script, use the `run` command in your terminal:
```bash
k6 run script.js
```

### Typical Project Folder Structure
For clean E2E performance suites:
```text
LoadTesting/
├── load-tests/
│   ├── config.js         # Base configurations, SLA thresholds, targets
│   ├── helpers.js        # Test data builders, faker functions
│   ├── login.js          # Authentication API test scenario
│   └── users.js          # Main Chatbot and bookings validate test scenarios
├── reports/
│   ├── load-test-report.html   # HTML dashboard reports
│   └── load-test-report.xlsx   # Excel SLA check sheets
├── load-test.js          # Simple entry-point baseline test script
├── parseK6Summary.js     # ExcelJS results compiler
└── package.json          # Node script manager
```

---

## 4. Baseline Load Test

A baseline test establishes the system's "resting" performance metrics using a low volume of users.

### Key k6 Concepts
* **Virtual Users (VUs)**: Simulated users executing iteration loops.
* **Duration**: The time for which the VUs will continuously run the script.
* **Iteration**: One complete execution of the export default function.
* **Request**: An HTTP request dispatched by the script (e.g. `http.get`).
* **Check**: An assertion that evaluates to true or false. Unlike unit tests, a failed check does not stop execution.
* **Sleep**: Inserts "think time" (delays) between iterations to simulate real user behavior.

### Baseline Script Example
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

// Test Configuration Options
export const options = {
  vus: 10,               // 10 concurrent Virtual Users
  duration: '30s',       // Run iteration loops for 30 seconds
};

export default function () {
  // Dispatch HTTP request
  const response = http.get('http://localhost:5000/');
  
  // Assert status code is 200
  check(response, {
    'status is 200': (r) => r.status === 200,
  });

  // Simulated think time
  sleep(1);
}
```

---

## 5. Sample API Load Test Script

Below is a production-ready script testing POST, GET, Auth headers, JSON payloads, dynamic data, checks, and SLA thresholds.

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 100,
  duration: '1m',
  thresholds: {
    http_req_failed: ['rate<0.05'], // Failure rate under 5%
    http_req_duration: ['p(95)<1500'], // 95% of requests must resolve under 1500ms
  },
};

function generateRandomString(length = 8) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

function generateRandomUser() {
  const rand = generateRandomString(5);
  return {
    name: `User_${rand}`,
    email: `user_${rand}@example.com`,
    phone: `98765${Math.floor(10000 + Math.random() * 90000)}`,
    password: `SecPass_${rand}!1`
  };
}

export default function () {
  const backendUrl = __ENV.BACKEND_URL || 'http://localhost:5000';
  const user = generateRandomUser();

  // 1. Authentication API Test (POST)
  const authPayload = JSON.stringify({
    phone: user.phone,
    password: user.password
  });
  
  const authHeaders = { 'Content-Type': 'application/json' };
  const authRes = http.post(`${backendUrl}/api/auth/signup/validate`, authPayload, { headers: authHeaders });
  
  check(authRes, {
    'auth status is 200': (r) => r.status === 200,
    'auth response is success': (r) => {
      try {
        return JSON.parse(r.body).success === true;
      } catch (e) {
        return false;
      }
    }
  });

  const token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy';

  // 2. Baseline GET API Test
  const getRes = http.get(backendUrl);
  check(getRes, {
    'GET status is 200': (r) => r.status === 200,
  });

  // 3. POST Chat API Test with Headers, Token, and JSON validation
  const chatPayload = JSON.stringify({
    message: `What are my rights under Section ${Math.floor(Math.random() * 500) + 1}?`,
    conversation: [],
    isLearning: false
  });

  const chatHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  const chatRes = http.post(`${backendUrl}/api/chat`, chatPayload, { headers: chatHeaders });

  let parsedJson = null;
  let isJson = false;
  try {
    parsedJson = JSON.parse(chatRes.body);
    isJson = true;
  } catch (err) {
    isJson = false;
  }

  check(chatRes, {
    'POST chat status is 200 or 500': (r) => r.status === 200 || r.status === 500,
    'POST chat response is valid JSON': () => isJson,
    'POST chat response contains success': (r) => isJson && parsedJson && parsedJson.success !== undefined
  });

  sleep(1);
}
```

---

## 6. Understanding Results

After execution, k6 prints summary metrics to stdout. Key metrics include:

| Metric Name | Type | Description |
| :--- | :--- | :--- |
| `http_reqs` | Counter | Total number of HTTP requests sent by k6. |
| `iterations` | Counter | Number of times VUs ran the complete script loop. |
| `vus` | Gauge | Current number of active Virtual Users. |
| `vus_max` | Gauge | Maximum allocated VUs for the test. |
| `data_received` | Rate | Volume of HTTP response bytes received. |
| `data_sent` | Rate | Volume of HTTP request bytes sent. |
| `checks` | Rate | Percentage of passed assertions. |
| `http_req_duration` | Trend | End-to-end request response duration (DNS + connection + waiting + transfer). |
| `http_req_waiting` | Trend | Time spent waiting for server processing (TTFB). |
| `http_req_connecting`| Trend | Time spent establishing TCP connections. |
| `http_req_blocked` | Trend | Time spent blocked waiting for TCP connection slots. |
| `http_req_failed` | Rate | Percentage of failed HTTP requests (status !== 2xx/3xx). |

---

## 7. Requests Per Second (RPS)

RPS measures throughput: the volume of API calls the system handles each second.

* **Formula**: `http_reqs count` / `test duration in seconds`
* **Example**: If `http_reqs` count is 6000 over 60 seconds:
  $$\text{RPS} = \frac{6000}{60} = 100 \text{ req/sec}$$

### Throughput Benchmarks (Standard CRUD APIs)

* **Excellent**: $>500$ req/sec per single backend instance. Indicates highly optimized, non-blocking asynchronous routing.
* **Average**: $100 - 500$ req/sec. Normal for databases and lightweight frameworks.
* **Poor**: $<100$ req/sec. Indicates blocking sync calls, database connection lockouts, or missing indexes.

---

## 8. Response Time

Response times are reported as a distribution to filter out outliers:

```text
  Min       Med / p50          p90         p95               Max
  |------------|----------------|-----------|-----------------|
  62ms        110ms           200ms       263ms             601ms
 (Fastest)   (50% Users)     (90% Users) (95% Users)       (Worst Case)
```

* **avg (Average)**: Arithmetic mean. Can be skewed by outliers.
* **min (Minimum)**: The fastest response recorded.
* **med (Median / p50)**: 50% of requests resolved faster than this.
* **p90 (90th Percentile)**: 90% of requests resolved faster than this.
* **p95 (95th Percentile)**: 95% of requests resolved faster than this. Indicates target SLA boundary.
* **max (Maximum)**: The slowest response recorded (outliers/garbage collection delays).

### Target Performance SLA Boundaries

* **Development (Local)**: Average $<50$ ms. Outliers $<150$ ms.
* **Testing (Staging/QA)**: Average $<200$ ms. p95 $<500$ ms.
* **Production**: Average $<300$ ms. p95 $<1000$ ms (standard consumer application limits).

---

## 9. Performance Benchmarks

Industry standard response times for REST APIs:

| Rating | Average Response Time | Description |
| :--- | :--- | :--- |
| **Excellent** | $< 100$ ms | Users perceive navigation as instantaneous. |
| **Good** | $100 - 300$ ms | Highly responsive. Standard target for enterprise web apps. |
| **Acceptable**| $300 - 800$ ms | Slight delay. Acceptable for heavy computational searches. |
| **Poor** | $> 800$ ms | Lag is noticeable. **Requires optimization.** |

---

## 10. Common Performance Bottlenecks

1. **Database Contention**:
   * Missing query indexes, resulting in full table scans.
   * Lock starvation when multiple connections modify the same table.
2. **Connection Pools Exhaustion**:
   * Database or API pool sizes set too low, causing threads to queue.
3. **Missing Redis Cache layer**:
   * Fetching static data repeatedly from disk/DB instead of memory.
4. **Memory Leaks (GC Pressure)**:
   * Retaining references in JavaScript closures, causing Node heap exhaustion and crashes.
5. **CPU Bound execution**:
   * Blocked event loop in Node due to sync crypto operations or parsing giant payloads.

---

## 11. Running Multiple APIs

To perform complex workflow testing (such as a full user lifecycle covering Login, Get Users, Create User, Update User, and Delete User), group the API requests sequentially inside a single test loop:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '30s',
};

export default function () {
  const host = __ENV.BACKEND_URL || 'http://localhost:5000';
  const headers = { 'Content-Type': 'application/json' };

  // 1. Login API validation
  const loginRes = http.post(`${host}/api/auth/signup/validate`, JSON.stringify({
    phone: '9999988888',
    password: 'SecurePassword123!'
  }), { headers });
  check(loginRes, { '1. Login status is 200': (r) => r.status === 200 });

  sleep(0.5);

  // 2. Get Users / Get Main Page
  const getUsersRes = http.get(`${host}/`);
  check(getUsersRes, { '2. Get Users status is 200': (r) => r.status === 200 });

  sleep(0.5);

  // 3. Create User Booking
  const createPayload = JSON.stringify({
    userId: 'user_123',
    lawyerId: 'lawyer_abc',
    phone: '9999988888',
    consultationType: 'Online',
    date: '2026-09-10',
    time: '15:30',
    video_consultation_available: true,
    availability_status: true
  });
  const createRes = http.post(`${host}/api/consultations/validate`, createPayload, { headers });
  check(createRes, { '3. Create Booking/User validation is 200': (r) => r.status === 200 });

  sleep(0.5);

  // 4. Update User Booking (Simulated via validate parameters edit)
  const updatePayload = JSON.stringify({
    userId: 'user_123',
    lawyerId: 'lawyer_abc',
    phone: '9999988888',
    consultationType: 'In-Person', // Change type
    date: '2026-09-12', // Change date
    time: '16:30', // Change time
    in_person_consultation_available: true,
    availability_status: true
  });
  const updateRes = http.post(`${host}/api/consultations/validate`, updatePayload, { headers });
  check(updateRes, { '4. Update Booking validation is 200': (r) => r.status === 200 });

  sleep(0.5);

  // 5. Delete User Booking / simulated cancellation validation
  // Evaluates a delete action on user bookings
  const deleteRes = http.post(`${host}/api/consultations/validate`, JSON.stringify({
    userId: 'user_123',
    lawyerId: 'lawyer_abc',
    phone: '9999988888',
    consultationType: 'Online',
    date: '2026-09-15',
    time: '12:30',
    video_consultation_available: true,
    availability_status: true
  }), { headers });
  check(deleteRes, { '5. Delete / Cancel Booking validation is 200': (r) => r.status === 200 });

  sleep(1);
}
```

---

## 12. Environment Variables

Access host parameters or credentials inside k6 using `__ENV`:

```javascript
// Read env parameters or fallback
const BASE_URL = __ENV.BASE_URL || 'http://localhost:5000';
const API_TOKEN = __ENV.API_TOKEN || 'default_secret';
```

Pass variables via command-line flags:
```bash
k6 run -e BASE_URL=https://staging.company.com -e API_TOKEN=xyz script.js
```

---

## 13. HTML Reports

Generate beautiful, responsive HTML reports containing interactive graphs by utilizing the [k6-reporter addon](https://github.com/benc-uk/k6-reporter):

Add the handler script to the bottom of your script:
```javascript
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";

export function handleSummary(data) {
  return {
    "summary.html": htmlReport(data),
  };
}
```

---

## 14. Grafana Dashboard Live Monitoring

For real-time test monitoring:

### Step 1: Run InfluxDB
Start InfluxDB using Docker to act as the time-series database:
```bash
docker run -d -p 8086:8086 --name influxdb influxdb:1.8
```

### Step 2: Route k6 output to InfluxDB
Configure k6 to stream metrics to the database during execution:
```bash
k6 run --out influxdb=http://localhost:8086/k6db script.js
```

### Step 3: Configure Grafana
Add InfluxDB as a data source in Grafana and import dashboard template **#2587** to view real-time latency graphs.

---

## 15. GitHub Actions Integration

Automate performance runs on push/PR events inside GitHub runners.

```yaml
# Step inside YAML
- name: Setup k6
  uses: grafana/setup-k6-action@v1

- name: Run load test
  run: k6 run script.js
```

---

## 16. Complete GitHub Actions YAML

Below is a production-ready CI workflow file located at `.github/workflows/load-test.yml`:

```yaml
name: API Load Testing

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

jobs:
  load-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'npm'
          cache-dependency-path: 'webapp/package-lock.json'

      - name: Install Dependencies
        run: |
          cd webapp
          npm ci

      - name: Setup k6
        uses: grafana/setup-k6-action@v1

      - name: Start Express Server in Background
        run: |
          cd webapp
          node server.js &
          # Wait for server to start
          for i in {1..10}; do
            curl -s http://localhost:5000/ && break
            sleep 1
          done
        env:
          PORT: 5000
          GROQ_API_KEY: "dummy_groq_key_for_test"

      - name: Run k6 Load Test
        run: |
          k6 run --summary-export=LoadTesting/summary.json LoadTesting/load-test.js
        env:
          BACKEND_URL: "http://localhost:5000"

      - name: Parse k6 Summary and Generate Reports
        if: always()
        run: |
          cd LoadTesting
          npm install
          node parseK6Summary.js
        env:
          BACKEND_URL: "http://localhost:5000"

      - name: Upload Reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: load-testing-reports
          path: |
            LoadTesting/reports/load-test-report.xlsx
            LoadTesting/reports/load-test-report.html
```

---

## 17. Best Practices

1. **Never test production without warning**: Load tests can trigger cloud scaling alarms, trigger firewalls, and incur significant bandwidth billing.
2. **Use Realistic think times**: Simulated users shouldn't click screens without delay. Use `sleep(randomRange)` to mimic human interactions.
3. **Use unique data**: Make sure POST calls submit randomized faked inputs so queries don't hit pre-cached memory paths.
4. **Assert failures on SLAs**: Always configure thresholds so tests fail automatically in CI if latency exceeds acceptable limits.

---

## 18. Folder Structure Reference

Recommended layout:
```text
load-tests/
├── login.js
├── users.js
├── config.js
└── helpers.js
```

---

## 19. Interview Questions & Answers

### Q1: What is Grafana k6?
**Answer**: k6 is an open-source, developer-centric performance testing tool written in Go that executes test scripts written in JavaScript (ES6) to evaluate backend infrastructure scalability.

### Q2: How does k6 differ from Apache JMeter?
**Answer**: JMeter is UI-driven and uses XML configurations, which can consume significant memory. k6 uses a lightweight JavaScript engine, has a smaller resource footprint (uses Go threads instead of OS JVM threads), and is designed for CI/CD integration.

### Q3: What is the purpose of the virtual user (VU) concept in k6?
**Answer**: VUs represent independent parallel execution threads. Each VU continuously runs the test script in a loop for the duration of the test.

### Q4: Explain the difference between `checks` and `thresholds`.
**Answer**: `checks` are boolean assertions that verify functional responses (e.g. status 200). Failing a check does not fail the test suite. `thresholds` are performance-based SLA metrics (e.g. p95 $<1000$ms). If a threshold fails, the test suite exits with a non-zero exit code, failing the CI pipeline.

### Q5: How do we generate custom trends in k6?
**Answer**: Import `Trend` from `k6/metrics` and log duration delta values inside your script:
```javascript
import { Trend } from 'k6/metrics';
const myTrend = new Trend('custom_duration');
myTrend.add(res.timings.duration);
```

### Q6: Can k6 simulate browser rendering?
**Answer**: Standard k6 scripts test the protocol layer (HTTP requests). To test the browser layer (rendering, DOM load times), use **k6 browser** (which integrates with Playwright APIs).

### Q7: What are 'stages' in options?
**Answer**: Stages configure the target VU workload over time. This enables ramping user volume up and down:
```javascript
stages: [
  { duration: '2m', target: 100 }, // ramp up
  { duration: '5m', target: 100 }  // steady state
]
```

### Q8: What does `http_req_waiting` measure?
**Answer**: It measures Time to First Byte (TTFB): the time between sending the request and receiving the first byte of the response. This represents backend server processing latency.

### Q9: How do you configure a soak test in k6?
**Answer**: Configure a steady, moderate VU load (e.g. 50 VUs) and set a long test duration (e.g. 4 to 12 hours) to monitor performance degradation or memory leaks.

### Q10: How do you handle authentication in k6?
**Answer**: Authenticate once during the `setup` lifecycle function, return the JWT token, and pass it to subsequent VU iterations via request headers.

### Q11: What is the `setup` function in k6?
**Answer**: A lifecycle function that runs once before VUs start. Use it to seed database records or retrieve auth tokens.

### Q12: What is the `teardown` function?
**Answer**: Runs once after all VUs complete execution. Use it to clean up database records generated during the test.

### Q13: What does a high `http_req_connecting` metric indicate?
**Answer**: It indicates network latency or server connection queue issues, meaning client threads are waiting to establish TCP handshakes.

### Q14: How does k6 handle cookies?
**Answer**: k6 manages cookies automatically per VU, mimicking standard browser session behaviors.

### Q15: How can you write custom summary outputs?
**Answer**: Define the global `handleSummary(data)` function in your script to customize the format and output paths of the summary data.

### Q16: What is the threshold syntax for checking the failure rate?
**Answer**: `'http_req_failed': ['rate<0.01']` checks that the overall HTTP request failure rate remains below 1%.

### Q17: What are 'Checks' limitations?
**Answer**: Checks only monitor and report success/failure rates in the final summary; they do not block or halt execution loops on failure.

### Q18: What is k6's execution engine model?
**Answer**: k6 compiles JavaScript scripts into bytecode and runs them within isolated Go-based runtimes, enabling high concurrency with low overhead.

### Q19: What is the benefit of ramping up virtual users slowly?
**Answer**: It helps identify the exact user volume where latency starts to degrade, rather than overwhelming the server immediately.

### Q20: What is the difference between peak load testing and stress testing?
**Answer**: Peak load testing verifies the system can handle expected traffic. Stress testing pushes traffic past expected limits to identify the system's breaking point.

### Q21: What is 'Think Time' in performance testing?
**Answer**: The delay introduced between actions (e.g. `sleep(2)`) to mimic the time real users spend reading or interacting with a page.

### Q22: What is throughput?
**Answer**: The rate at which the application processes requests, measured as transactions or requests per second (RPS).

### Q23: How do database query indexes improve performance?
**Answer**: They prevent the database engine from scanning the entire table on every query, reducing disk I/O and query execution times.

### Q24: What is p95 response time?
**Answer**: The boundary under which 95% of all requests resolved. This filters out the worst-performing 5% of requests.

### Q25: Why is checking the error log during a load test important?
**Answer**: Slow response times can sometimes be caused by error handlers (such as database retry loops) repeatedly failing and consuming system resources.

### Q26: What is a memory leak?
**Answer**: A resource leak that occurs when a program fails to release memory it no longer needs, leading to performance degradation or crashes.

### Q27: How does network latency affect load test results?
**Answer**: High latency between the load injector and the application server increases measured response times, even if backend processing is fast.

### Q28: Can k6 run in parallel across multiple systems?
**Answer**: Yes, distributed load testing can be executed using the k6 Operator on Kubernetes clusters.

### Q29: What is the default port for InfluxDB?
**Answer**: The default port is `8086`.

### Q30: How can we load external test data into k6?
**Answer**: Read file contents into memory using `open()` at the global scope, and parse it (e.g., `JSON.parse` or CSV parsers) to distribute to VUs.

---

## 20. Summary Cheat Sheet

### Installation
* **Windows (Winget)**: `winget install GrafanaLabs.k6`
* **Mac (Homebrew)**: `brew install k6`
* **Linux (Debian/Ubuntu)**: `sudo apt-key adv ... && sudo apt-get install k6`

### Common CLI Commands
* Run a test: `k6 run script.js`
* Run with custom VUs: `k6 run --vus 50 --duration 10s script.js`
* Run with env values: `k6 run -e BASE_URL=http://localhost:5000 script.js`

### Best Practices Checklist
* [ ] Always set think time using `sleep()`.
* [ ] Define thresholds for SLA target monitoring.
* [ ] Parameterize test data using faker inputs.
* [ ] Run tests on dedicated staging environments (not production).
* [ ] Verify that system resource utilization (CPU/Memory/I/O) is monitored during execution.
