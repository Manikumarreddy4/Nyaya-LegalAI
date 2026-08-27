import http from 'k6/http';
import { check, sleep } from 'k6';
import config from './config.js';
import { generateRandomUser } from './helpers.js';

export const options = {
  vus: 100,
  duration: '1m',
  thresholds: config.thresholds
};

export default function () {
  const user = generateRandomUser();
  const payload = JSON.stringify({
    phone: user.phone,
    password: user.password
  });

  const params = {
    headers: config.defaultHeaders
  };

  const response = http.post(`${config.baseUrl}/api/auth/signup/validate`, payload, params);

  check(response, {
    'status is 200': (r) => r.status === 200,
    'validation success': (r) => JSON.parse(r.body).success === true
  });

  sleep(1);
}
