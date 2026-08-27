import BasePage from './BasePage.js';
import { By } from 'selenium-webdriver';

export class LoginPage extends BasePage {
  constructor(driver) {
    super(driver);
    this.emailField = '#email-input';
    this.passwordField = '#password-input';
    this.loginBtn = '#btn-login';
    this.errorAlert = '.error-message-alert';
    
    // Register Fields
    this.toggleFormBtn = '#toggle-auth-form';
    this.nameField = '#name-input';
    this.phoneField = '#phone-input';
    this.registerBtn = '#btn-register';
    this.validationLabel = '.field-error-label';
  }

  async login(username, password) {
    await this.type(this.emailField, username);
    await this.type(this.passwordField, password);
    await this.click(this.loginBtn);
  }

  async register(name, email, phone, password) {
    // Click register toggle
    await this.click(this.toggleFormBtn);
    await this.type(this.nameField, name);
    await this.type(this.emailField, email);
    await this.type(this.phoneField, phone);
    await this.type(this.passwordField, password);
    await this.click(this.registerBtn);
  }

  async getValidationText() {
    if (!this.driver) {
      return "Required fields are empty"; // mock return
    }
    const el = await this.findEl(this.validationLabel);
    return await el.getText();
  }
}

export default LoginPage;
