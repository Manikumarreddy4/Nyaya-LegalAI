import BasePage from './BasePage.js';
import { By } from 'selenium-webdriver';

export class DashboardPage extends BasePage {
  constructor(driver) {
    super(driver);
    // Navigation Links
    this.dashboardLink = '#nav-dashboard';
    this.assistantLink = '#nav-assistant';
    this.learningLink = '#nav-learning';
    this.encyclopediaLink = '#nav-encyclopedia';
    this.findLawyerLink = '#nav-find-lawyer';
    this.bookingsLink = '#nav-bookings';
    this.profileLink = '#nav-profile';
    this.logoutBtn = '#btn-logout';

    // UI Widgets & Cards
    this.welcomeHeader = '.welcome-header-title';
    this.assistantCard = '#card-legal-assistant';
    this.bookingModal = '#booking-form-modal';
    this.bookingPhoneInput = '#booking-phone';
    this.bookingDateInput = '#booking-date';
    this.bookingTimeInput = '#booking-time';
    this.bookingSubmitBtn = '#btn-confirm-booking';
    this.bookingSuccessToast = '.toast-booking-success';

    // Advocate availability toggle
    this.lawyerStatusToggle = '#lawyer-availability-switch';
  }

  async navigateTo(linkSelector) {
    await this.click(linkSelector);
  }

  async bookSlot(phone, date, time) {
    await this.click(this.findLawyerLink);
    // Click advocate profile
    await this.click('.advocate-profile-card');
    await this.click('#btn-schedule-meeting');
    // Fill schedule form modal
    await this.type(this.bookingPhoneInput, phone);
    await this.type(this.bookingDateInput, date);
    await this.type(this.bookingTimeInput, time);
    await this.click(this.bookingSubmitBtn);
  }
}

export default DashboardPage;
