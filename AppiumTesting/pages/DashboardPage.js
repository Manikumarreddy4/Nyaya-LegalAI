import BasePage from './BasePage.js';
import { find } from '../driver/driverFactory.js';

export class DashboardPage extends BasePage {
  constructor(client) {
    super(client);
    
    // Quick Actions
    this.legalAssistantCard = find.byValueKey('card_assistant');
    this.legalLearningCard = find.byValueKey('card_learning');
    this.encyclopediaCard = find.byValueKey('card_encyclopedia');
    
    // Booking Elements
    this.findLawyerTab = find.byText('Find Lawyer');
    this.lawyerCard = find.byText('Adv. Rajesh Kumar');
    this.bookSlotButton = find.byValueKey('btn_book_consultation');
    this.phoneInput = find.byValueKey('input_booking_phone');
    this.onlineRadio = find.byText('Online');
    this.inPersonRadio = find.byText('In-Person');
    this.datePicker = find.byValueKey('picker_date');
    this.timePicker = find.byValueKey('picker_time');
    this.confirmBookingBtn = find.byValueKey('btn_confirm_booking');
    this.bookingSuccessMsg = find.byText('Booking Successful!');
    
    // Lawyer Mode
    this.availabilityToggle = find.byValueKey('switch_availability');
    this.acceptRequestBtn = find.byValueKey('btn_accept');
    this.rejectRequestBtn = find.byValueKey('btn_reject');
    
    // Tabs & History Filters
    this.myBookingsTab = find.byText('My Bookings');
    this.filterPending = find.byText('Pending');
    this.filterAccepted = find.byText('Accepted');
    this.filterRejected = find.byText('Rejected');
    
    // Drawer/Menu Toggle
    this.menuButton = find.byAccessibilityId('Open navigation drawer');
    this.logoutButton = find.byText('Logout');
  }

  async bookConsultation(phone, type, date, time) {
    await this.click(this.findLawyerTab);
    await this.click(this.lawyerCard);
    await this.click(this.bookSlotButton);
    await this.type(this.phoneInput, phone);
    if (type === 'Online') {
      await this.click(this.onlineRadio);
    } else {
      await this.click(this.inPersonRadio);
    }
    await this.click(this.datePicker);
    await this.click(this.timePicker);
    await this.click(this.confirmBookingBtn);
  }

  async logout() {
    await this.click(this.menuButton);
    await this.click(this.logoutButton);
  }
}

export default DashboardPage;
