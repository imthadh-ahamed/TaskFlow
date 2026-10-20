# Database security rules

- Never commit real passwords, connection strings, tokens, certificates, or production secrets.
- Keep local environment files out of Git; commit placeholders only.
- Use least-privilege database roles for applications and migrations where appropriate.
- Do not use a superuser account for ordinary application connections.
- Treat user input as untrusted; use parameterized values and safe identifier handling.
- Review grants and execution permissions for procedures and functions.
- Avoid `SECURITY DEFINER` unless necessary and ensure a safe `search_path` if used.
- Do not expose sensitive data through seed files, logs, test fixtures, or documentation.
- Ask before changing credentials, permissions, shared environments, or production data.
