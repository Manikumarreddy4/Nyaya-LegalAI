# Backend Service API Gateway Inventory

Comprehensive overview of all supported REST API routes within the Nyaya LegalAI project ecosystem.

## API Endpoint Table

| Route Pattern | HTTP Method | CORS Config | Auth Requirement | Target Service / Manager | Risk Level |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `/` | `GET` | Wildcard `*` | Public (None) | System Health Check | Low |
| `/api/auth/signup` | `POST` | Wildcard `*` | Public (None) | User Profile Database | Medium |
| `/api/auth/login` | `POST` | Wildcard `*` | Public (None) | JWT Sign-in Issuer | High |
| `/api/auth/validate` | `POST` | Wildcard `*` | Authorization Bearer | Token Validator | Medium |
| `/api/chat` | `POST` | Wildcard `*` | Authorization Bearer | LegalAssistantManager | High |
| `/api/bookings/slots` | `GET` | Wildcard `*` | Public (None) | Advocate Available Calendar | Low |
| `/api/bookings` | `POST` | Wildcard `*` | Authorization Bearer | Consultation Booking Manager | Medium |
