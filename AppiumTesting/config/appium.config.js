import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export const appiumConfig = {
  hostname: process.env.APPIUM_HOST || '127.0.0.1',
  port: parseInt(process.env.APPIUM_PORT || '4723', 10),
  path: '/',
  capabilities: {
    platformName: 'Android',
    'appium:deviceName': process.env.DEVICE_NAME || 'Android Emulator',
    'appium:automationName': 'UiAutomator2',
    'appium:app': process.env.APK_PATH || path.resolve(__dirname, '../../app/build/outputs/apk/release/app-release.apk'),
    'appium:appPackage': process.env.APP_PACKAGE || 'com.example.nyayalegalai',
    'appium:appActivity': process.env.APP_ACTIVITY || 'com.example.nyayalegalai.MainActivity',
    'appium:noReset': true,
    'appium:autoGrantPermissions': true,
    'appium:newCommandTimeout': 300,
    'appium:adbExecTimeout': 60000
  }
};

export default appiumConfig;
