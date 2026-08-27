import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 100,
  duration: '1m',
  thresholds: {
    http_req_failed: ['rate<0.05'], // request failures under 5%
    http_req_duration: ['p(95)<1500'], // 95th percentile under 1500ms
  },
};

export default function () {
  const url = __ENV.BACKEND_URL || 'http://localhost:5000';
  const res = http.get(url);
  
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  
  sleep(1);
}
