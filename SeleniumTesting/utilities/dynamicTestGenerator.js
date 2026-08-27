import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { logger } from './logger.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export class DynamicTestGenerator {
  static discoverRoutesAndValidation() {
    logger.info('[DynamicTestGenerator] Running React routes and forms discovery scan...');
    const appPath = path.resolve(__dirname, '../../webapp/src/App.jsx');
    const serverPath = path.resolve(__dirname, '../../webapp/server.js');

    const discoveredRoutes = [];
    const discoveredValidationRules = [];

    // 1. Scan React Routes in App.jsx
    if (fs.existsSync(appPath)) {
      const appContent = fs.readFileSync(appPath, 'utf8');
      
      // Match case 'route-name': patterns in renderPage switch
      const routeRegex = /case\s+'([^']+)':/g;
      let match;
      while ((match = routeRegex.exec(appContent)) !== null) {
        if (!discoveredRoutes.includes(match[1])) {
          discoveredRoutes.push(match[1]);
        }
      }
      logger.info(`[DynamicTestGenerator] Discovered React web routes: ${discoveredRoutes.join(', ')}`);
    } else {
      logger.warn('[DynamicTestGenerator] React App.jsx not found. Using default routes fallback.');
      discoveredRoutes.push('login', 'dashboard', 'profile', 'find-lawyer', 'my-bookings');
    }

    // 2. Scan validation rules in webapp/server.js or forms
    if (fs.existsSync(serverPath)) {
      const serverContent = fs.readFileSync(serverPath, 'utf8');
      
      // Find regex validators
      const regexPatterns = /const\s+(\w+Pattern)\s*=\s*(\/[^\/]+\/)/g;
      let pMatch;
      while ((pMatch = regexPatterns.exec(serverContent)) !== null) {
        discoveredValidationRules.push({
          name: pMatch[1],
          pattern: pMatch[2],
          context: pMatch[1].includes('phone') ? 'Phone Number Validation' : 'Password Complexity'
        });
      }
      
      // Find text error messages
      const errorMsgRegex = /error:\s*"([^"]+)"/g;
      let eMatch;
      while ((eMatch = errorMsgRegex.exec(serverContent)) !== null) {
        if (discoveredValidationRules.length < 5) {
          discoveredValidationRules.push({
            name: 'API Error Response Check',
            message: eMatch[1]
          });
        }
      }
    }

    // 3. Compile dynamic test cases based on validation rules
    const dynamicTests = [];
    
    // Auth Validation Tests
    dynamicTests.push({
      id: 'DYN-WEB-001',
      module: 'Authentication',
      scenarioName: 'Dynamic verification: Attempt login with empty fields',
      steps: 'Navigate to Login, enter empty username/password, and verify validation blocker message displays.',
      expected: 'Validation error alert is rendered on login form.'
    });

    discoveredValidationRules.forEach((rule, idx) => {
      if (rule.name.toLowerCase().includes('phone')) {
        dynamicTests.push({
          id: `DYN-WEB-PH-${idx + 1}`,
          module: 'Form Validation',
          scenarioName: `Dynamic format check matching '${rule.name}' regex ${rule.pattern}`,
          steps: `Fill phone field input violating regex ${rule.pattern} and attempt form submission.`,
          expected: 'Phone validation message "must contain exactly 10 digits" is displayed.'
        });
      } else if (rule.name.toLowerCase().includes('password')) {
        dynamicTests.push({
          id: `DYN-WEB-PW-${idx + 1}`,
          module: 'Form Validation',
          scenarioName: `Dynamic password strength check using regex ${rule.pattern}`,
          steps: `Input password violating complexity schema ${rule.pattern} and check for warning.`,
          expected: 'Password constraint alerts are rendered.'
        });
      }
    });

    // Navigation Tests from routes
    discoveredRoutes.forEach((route, idx) => {
      dynamicTests.push({
        id: `DYN-NAV-${route.toUpperCase()}-${idx + 1}`,
        module: 'Navigation Routing',
        scenarioName: `Verify route transitions and body container loading for /${route}`,
        steps: `Log in user and click sidebar link directing to route path: ${route}.`,
        expected: `Redirects URL state and mounts the /${route} container view.`
      });
    });

    logger.info(`[DynamicTestGenerator] Generated ${dynamicTests.length} dynamic E2E validation test cases.`);
    return dynamicTests;
  }
}

export default DynamicTestGenerator;
