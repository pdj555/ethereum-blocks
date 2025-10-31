# Security Policy

## Supported Versions

We release patches for security vulnerabilities in the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 2.0.x   | :white_check_mark: |
| < 2.0   | :x:                |

## Reporting a Vulnerability

We take the security of the Ethereum Block Explorer seriously. If you believe you have found a security vulnerability, please report it to us as described below.

### Please Do Not:
- Open a public GitHub issue for security vulnerabilities
- Discuss the vulnerability in public forums or social media

### Please Do:
1. **Report via GitHub Security Advisories** (preferred)
   - Go to the repository's Security tab
   - Click "Report a vulnerability"
   - Fill out the form with details

2. **Or report via email** to the project maintainers
   - Include detailed information about the vulnerability
   - Provide steps to reproduce
   - Include potential impact assessment

### What to Include:
- Type of vulnerability
- Full paths of source file(s) related to the vulnerability
- Location of the affected source code (tag/branch/commit)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the vulnerability

### Response Timeline:
- **Within 48 hours**: We will acknowledge receipt of your report
- **Within 7 days**: We will provide a detailed response with next steps
- **Within 30 days**: We will work with you to validate and fix the issue

## Security Best Practices

### For Deployment:
1. **Run as non-privileged user**
   - Never run the application as root
   - Create dedicated service account

2. **File Permissions**
   ```bash
   chmod 640 logs/*.log
   chmod 644 *.csv
   chmod 755 *.sh
   ```

3. **Data Validation**
   - Keep `validation.strict=true` in production
   - Validate all input data files
   - Check data file integrity before loading

4. **JVM Security**
   ```bash
   -Djava.security.manager
   -Djava.security.policy=security.policy
   ```

5. **Logging**
   - Never log sensitive data
   - Implement log rotation
   - Monitor logs for anomalies

### Known Security Features:
- ✅ Input validation for all user inputs
- ✅ Ethereum address format validation
- ✅ Numeric bounds checking
- ✅ Exception handling without exposing internals
- ✅ No hardcoded credentials
- ✅ Safe file handling with try-with-resources
- ✅ Defensive copying to prevent external modification

### Dependencies:
We use Maven to manage dependencies and regularly update them to patch security vulnerabilities.

Check for dependency vulnerabilities:
```bash
mvn dependency:analyze
mvn versions:display-dependency-updates
```

## Security Considerations

### Input Data:
- CSV files are parsed with validation
- Invalid entries are logged and skipped
- No code execution from data files
- File paths are validated

### Memory Safety:
- Bounded collections to prevent memory exhaustion
- Proper resource cleanup with try-with-resources
- No unbounded recursion

### Thread Safety:
- This application is designed for single-threaded CLI use
- Multi-threaded use requires external synchronization

## Disclosure Policy

When we receive a security report, we will:
1. Confirm the problem and determine affected versions
2. Audit code to find similar problems
3. Prepare fixes for all supported versions
4. Release patched versions as soon as possible

## Comments on this Policy

If you have suggestions for improving this policy, please open an issue or submit a pull request.

---

**Last Updated**: October 2025
