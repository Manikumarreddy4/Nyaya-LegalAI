import http from 'k6/http';
import { check, sleep } from 'k6';
import config from './config.js';
import { generateRandomString } from './helpers.js';

export const options = {
  vus: 50,
  duration: '1m',
  thresholds: config.thresholds
};

export default function () {
  // 1. Post legal chat message to AI assistant
  const chatPayload = JSON.stringify({
    message: 'What are basic rights under Article 21 of Indian constitution?',
    conversation: [
      { role: 'user', content: 'Hello AI' },
      { role: 'assistant', content: 'Hello, how can I assist you with Indian law today?' }
    ],
    isLearning: false
  });

  const params = {
    headers: config.defaultHeaders
  };

  const chatResponse = http.post(`${config.baseUrl}/api/chat`, chatPayload, params);

  check(chatResponse, {
    'chat response status is 200/500': (r) => r.status === 200 || r.status === 500,
    'chat response is json': (r) => {
      try {
        JSON.parse(r.body);
        return true;
      } catch (e) {
        return false;
      }
    }
  });

  // 2. Post consultation validations checks
  const bookingPayload = JSON.stringify({
    userId: 'test_user_id_' + generateRandomString(4),
    lawyerId: 'test_lawyer_id_' + generateRandomString(4),
    phone: '9876543210',
    consultationType: 'Online',
    date: '2026-09-15',
    time: '11:30',
    video_consultation_available: true,
    availability_status: true
  });

  const bookingResponse = http.post(`${config.baseUrl}/api/consultations/validate`, bookingPayload, params);

  check(bookingResponse, {
    'booking status is 200': (r) => r.status === 200,
    'booking validate success': (r) => JSON.parse(r.body).success === true
  });

  sleep(1);
}
