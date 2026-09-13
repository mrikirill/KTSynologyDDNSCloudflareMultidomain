# Contributing to KTSynologyDDNSCloudflareMultidomain

First off, thanks for taking the time to contribute! 🎉

The following is a set of guidelines for contributing to this project. These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in a pull request.

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

- **Use a clear and descriptive title** for the issue to identify the problem.
- **Describe the exact steps to reproduce the problem** in as much detail as possible.
- **Provide specific examples** to demonstrate the steps.
- **Describe the behavior you observed** after following the steps and point out what exactly is the problem with that behavior.
- **Explain which behavior you expected to see** instead and why.
- **Include logs** if possible (sanitize them to remove sensitive API keys or domains).

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion, including completely new features and minor improvements to existing functionality.

- **Use a clear and descriptive title** for the issue to identify the suggestion.
- **Provide a step-by-step description of the suggested enhancement** in as much detail as possible.
- **Explain why this enhancement would be useful** to most users.

### Pull Requests

1.  Fork the repo and create your branch from `master`.
2.  If you've added code that should be tested, add tests.
3.  Ensure the test suite passes.
4.  Make sure your code follows the existing code style.
5.  Issue that pull request!

## Development Setup

### Prerequisites

-   **JDK 21**: This project uses Java 21.
-   **Docker**: Required if you want to build Linux binaries on macOS or Windows.

### Building Locally

To build the project for your local machine (e.g., macOS):

```bash
./gradlew build
```

### Running Tests

To run the unit tests:

```bash
./gradlew check
```

### Testing the Installer

Installer tests require Python 3 and the shell being tested, but no Synology
device, root access, or network connection:

```sh
INSTALL_TEST_SHELL=dash PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tests -v
shellcheck --shell=sh install.sh
```

CI also runs these tests with `INSTALL_TEST_SHELL='busybox sh'` and `bash`.
The tests redirect installation paths into a temporary directory and stub
architecture detection, downloads, and privilege elevation. They verify ARM64
and x64 asset selection, permissions, provider configuration output, unsupported
architectures, and download failure handling. They do not run the native agent
or verify SRM's utilities, existing provider replacement, or DDNS integration.

For device QA, use the candidate `install.sh` (the `master` download command
will only include the fix after it is merged). Record the model, SRM version,
and `uname -m`; run `sudo sh install.sh`; confirm that Cloudflare appears in
the DDNS provider list; then test a disposable DNS record and verify its value
in Cloudflare. Share the installer output and DDNS result with credentials
removed. Shell tests alone should not be reported as full SRM certification.

### Building for Linux (Cross-Compilation)

Since this is a Kotlin Native project, building Linux binaries on macOS requires Docker.

**For Linux X64 (Synology DSM):**
```bash
docker buildx build --platform linux/amd64 -t ktsynology-linux-x64 .
```

**For Linux ARM64 (Synology SRM):**
```bash
docker buildx build --platform linux/arm64 -t ktsynology-linux-arm64 .
```
