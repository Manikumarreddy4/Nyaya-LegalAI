export const config = {
  baseUrl: __ENV.BASE_URL || 'http://localhost:5000',
  defaultHeaders: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  thresholds: {
    http_req_failed: ['rate<0.05'], // Request failure rate under 5%
    http_req_duration: ['p(95)<1500'] // p(95) response time under 1500ms
  }
};

export default config;
