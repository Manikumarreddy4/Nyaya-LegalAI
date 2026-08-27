@echo off
:menu
cls
echo ====================================================
echo      NYAYA LEGAL AI - E2E & PERFORMANCE SUITE
echo ====================================================
echo.
echo [1] Install Selenium Web E2E dependencies
echo [2] Run 1200 Web E2E Selenium Tests
echo [3] Install Appium Mobile E2E dependencies
echo [4] Run 1200 Mobile E2E Appium Tests
echo [5] Install k6 load tester (Windows Winget)
echo [6] Run 100-User API Load Test (k6)
echo [7] Open Web E2E Reports (Excel & HTML)
echo [8] Open Mobile E2E Reports (Excel & HTML)
echo [9] Open API Load Test Reports (Excel & HTML)
echo [10] Exit
echo.
set /p opt="Choose option (1-10): "

if "%opt%"=="1" (
    echo.
    echo [*] Installing Web Selenium E2E dependencies...
    cd SeleniumTesting && npm install && cd ..
    echo.
    echo [+] Installation complete.
    pause
    goto menu
)
if "%opt%"=="2" (
    echo.
    echo [*] Launching 1200 Web E2E Selenium Tests...
    cd SeleniumTesting && npm run test && cd ..
    pause
    goto menu
)
if "%opt%"=="3" (
    echo.
    echo [*] Installing Mobile Appium E2E dependencies...
    cd AppiumTesting && npm install && cd ..
    echo.
    echo [+] Installation complete.
    pause
    goto menu
)
if "%opt%"=="4" (
    echo.
    echo [*] Launching 1200 Mobile E2E Appium Tests...
    cd AppiumTesting && npm run test && cd ..
    pause
    goto menu
)
if "%opt%"=="5" (
    echo.
    echo [*] Installing k6 load testing utility...
    winget install --id Grafana.k6 --silent
    echo.
    echo [+] k6 install complete (if winget is configured).
    pause
    goto menu
)
if "%opt%"=="6" (
    echo.
    echo [*] Running 100-User 1-Minute API Load Test...
    echo Note: Ensure Express server (webapp/server.js) is running on port 5000 first!
    cd LoadTesting
    k6 run --summary-export=summary.json load-test.js
    node parseK6Summary.js
    cd ..
    pause
    goto menu
)
if "%opt%"=="7" (
    echo Opening Web Reports...
    start SeleniumTesting/reports/excel/selenium-test-report.xlsx
    start SeleniumTesting/reports/html/execution-report.html
    goto menu
)
if "%opt%"=="8" (
    echo Opening Mobile Reports...
    start AppiumTesting/reports/excel/appium-test-report.xlsx
    start AppiumTesting/reports/html/execution-report.html
    goto menu
)
if "%opt%"=="9" (
    echo Opening Load Test Reports...
    start LoadTesting/reports/load-test-report.xlsx
    start LoadTesting/reports/load-test-report.html
    goto menu
)
if "%opt%"=="10" (
    exit
)
goto menu
