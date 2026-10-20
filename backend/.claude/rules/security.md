# Security rules

- Never store credentials, API keys, private keys, or production secrets in source control.
- Do not log passwords, authorization headers, access tokens, session IDs, or sensitive personal data.
- Validate and normalize untrusted input at system boundaries.
- Use password hashing designed for passwords if authentication is added; never use plain-text or reversible encryption for passwords.
- Enforce authorization on the server for every protected resource; hiding a UI control is not authorization.
- Configure CORS narrowly for known frontend origins; do not use permissive wildcard origins with credentials.
- If cookie-based authentication is used, assess CSRF protections and secure cookie flags.
- If bearer tokens are used, define expiry, storage, validation, and revocation strategy deliberately.
- Avoid returning internal exception details to clients.
- Ask before changing live infrastructure, production data, or secrets.
