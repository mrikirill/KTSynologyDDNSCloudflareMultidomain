# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Support for Linux ARM64 architecture (Synology SRM devices).
- Docker build support for cross-compilation on macOS/Windows.
- `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md`.

### Changed
- Updated Kotlin to 2.2.21.
- Updated Ktor to 3.3.3.
- Updated Kotlinx Serialization to 1.9.0.
- Updated Kotlinx Coroutines to 1.10.2.
- Updated Gradle Wrapper to 8.11.1.
- Updated `install.sh` to support ARM64 architecture.
- Updated `README.md` with new build instructions and corrections.

## [1.0.0] - 2021-10-24
### Added
- Initial release.
- Support for Synology DSM (Linux X64).
- Cloudflare API v4 integration.
- IPv4 and IPv6 support.
