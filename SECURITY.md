# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |

## Reporting a Vulnerability

To report a security vulnerability, **do not** open a public GitHub issue, as this
could expose the vulnerability to others before a fix is available.

Instead, use **GitHub Private Vulnerability Reporting**:

1. Go to the **Security** tab of this repository
2. Click **Report a vulnerability**
3. Fill in the details and submit

### What to Include

- Description of the vulnerability and its potential impact
- Steps to reproduce (proof-of-concept if possible)
- Affected versions
- Suggested mitigation or fix (optional)

### Response Timeline

| Event                   | Target                          |
| ----------------------- | ------------------------------- |
| Initial acknowledgement | Within 72 hours                 |
| Status update           | Within 7 days                   |
| Fix released            | Within 90 days of confirmation  |

### Out of Scope

- Vulnerabilities in third-party dependencies (report upstream to those projects)
- Issues that require physical access to server infrastructure
- Social engineering or phishing attacks
- Denial-of-service attacks against publicly accessible endpoints

## Security Updates

Security fixes are applied to the latest stable release only.
Keep dependencies up to date using the automated Dependabot pull requests.

## Attribution

Responsible disclosure is greatly appreciated. Unless you request anonymity,
security reporters will be credited in the release notes for the fix.