# Security and Performance Remediation Guide

Actionable steps to resolve flagged issues in security CORS settings and performance SLA latency spikes.

## Flagged Item 1: Permissive CORS Configuration
- **Vulnerability**: Endpoints return `Access-Control-Allow-Origin: *` headers, allowing any site to query local resources.
- **Remediation**:
  - Replace wildcard origins with an explicit, secure domain whitelist.
  - Implement dynamic origin checks to confirm request headers match valid company domains.

## Flagged Item 2: Latency SLA Violations
- **Bottleneck**: Database connection pools exhaust under high concurrent loads, causing latency spikes.
- **Remediation**:
  - Increase the SQL connection pool limit on the Express/FastAPI backends.
  - Introduce cache headers and Redis clusters to store advocate calendar slots.
