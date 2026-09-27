# Security & Privacy Policy

## Credential Protection
- API keys are injected at build time using the Secrets Gradle Plugin from `.env` and `.env.example`.
- `.env` is strictly excluded from version control via `.gitignore`.
- Private credentials and user API keys are never written to unencrypted log files.

## APK Key Extraction Warning
Android APKs can be decompiled. If a user enters their own private API key into the local settings:
- Keys are kept in memory and local app storage.
- Never share generated debug APKs publicly if they contain private personal API keys.

## Trade Execution Isolation
- Aegis AI Desk is exclusively an Intelligence & Decision Support System.
- There is NO direct broker order execution capability or automated money transfer functionality in this release, preventing unauthorized trades or algorithmic runaway errors.
