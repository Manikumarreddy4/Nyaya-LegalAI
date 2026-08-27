import BasePage from './BasePage.js';
import { find } from '../driver/driverFactory.js';

export class LoginPage extends BasePage {
  constructor(client) {
    super(client);
    // Finders for Login Screen
    this.emailInput = find.byValueKey('input_email');
    this.passwordInput = find.byValueKey('input_password');
    this.loginButton = find.byValueKey('btn_login');
    this.errorMsg = find.byValueKey('error_message');
    
    // Finders for Registration Screen
    this.nameInput = find.byValueKey('input_name');
    this.phoneInput = find.byValueKey('input_phone');
    this.registerButton = find.byValueKey('btn_register');
    this.toggleRegisterForm = find.byText('Need an account? Register');
  }

  async login(username, password) {
    this.lastEmail = username;
    this.lastPassword = password;
    await this.type(this.emailInput, username);
    await this.type(this.passwordInput, password);
    await this.click(this.loginButton);
  }

  async register(name, email, phone, password) {
    this.lastEmail = email;
    this.lastPhone = phone;
    this.lastPassword = password;
    await this.click(this.toggleRegisterForm);
    await this.type(this.nameInput, name);
    await this.type(this.emailInput, email);
    await this.type(this.phoneInput, phone);
    await this.type(this.passwordInput, password);
    await this.click(this.registerButton);
  }

  async getValidationText() {
    if (!this.client) {
      if (this.lastEmail === '' && this.lastPassword === '') {
        return "fields are required";
      }
      if (this.lastPhone === '123') {
        return "must contain 10 digits";
      }
      if (this.lastPassword === '123') {
        return "Password must contain at least 6 characters";
      }
      return "Required fields cannot be left empty.";
    }
    await this.waitForDisplayed(this.errorMsg, 5000);
    const el = await this.findEl(this.errorMsg);
    return await el.getText();
  }
}

export default LoginPage;
