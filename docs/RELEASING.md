# Releasing & CI/CD Guide

Vesper uses single-source semantic versioning paired with automated GitHub Actions workflows for Continuous Integration (`.github/workflows/ci.yml`) and Release builds (`.github/workflows/release.yml`).

---

## Versioning Model

Base version fields live in [`version.properties`](../version.properties) at the repository root:

```properties
VERSION_MAJOR=1
VERSION_MINOR=0
VERSION_PATCH=0
VERSION_CODE=1
```

[`app/build.gradle.kts`](../app/build.gradle.kts) resolves `versionName` and `versionCode` in the following precedence order:

1. Gradle properties: `-PappVersionName=...` and `-PappVersionCode=...`
2. Environment variables: `APP_VERSION_NAME` and `APP_VERSION_CODE`
3. Fallback values from `version.properties`

### Automated Version Derivation in CI

- **CI builds (`ci.yml`):**
  - `versionName`: `<major>.<minor>.<patch>-ci.<run_number>.<short_sha>` (or `-pr.<pr_number>.<short_sha>` on pull requests)
  - `versionCode`: `VERSION_CODE + GITHUB_RUN_NUMBER`
- **Release builds (`release.yml`):**
  - `versionName`: Stripped from the Git tag (e.g. `v1.2.0` → `1.2.0`), or supplied via manual `workflow_dispatch` input
  - `versionCode`: Monotonically calculated as:
    `MAJOR * 1,000,000 + MINOR * 10,000 + PATCH * 100 + (git_commit_count % 100)`
    (e.g., `v1.2.0` at commit `42` yields `1020042`).

---

## GitHub Secrets for Signed Production Releases

To sign Release `.apk` and `.aab` artifacts with your production upload keystore in `.github/workflows/release.yml`, add these three **Repository Secrets** (`Settings → Secrets and variables → Actions`):

| Secret Name | Description | How to Generate |
| :--- | :--- | :--- |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded `.jks` keystore (`alias = upload`) | `base64 -w 0 my-upload-key.jks` (Linux) or `base64 -i my-upload-key.jks \| tr -d '\n'` (macOS) |
| `RELEASE_STORE_PASSWORD` | Keystore password (`STORE_PASSWORD`) | Keystore creation password |
| `RELEASE_KEY_PASSWORD` | Key password for the `upload` alias (`KEY_PASSWORD`) | Key alias password |

> **Note:** If production signing secrets are not yet configured, `release.yml` automatically generates a temporary ephemeral keystore and labels outputs `-ci-signed` so release verification never fails on forks or initial setups. Replace with production secrets before uploading to Google Play.

---

## Creating a New Release

1. Update `version.properties` and `CHANGELOG.md` on `main`:
   ```properties
   VERSION_MAJOR=1
   VERSION_MINOR=1
   VERSION_PATCH=0
   VERSION_CODE=1010000
   ```
2. Commit and push to `main`.
3. Tag the release and push the tag:
   ```bash
   git tag -a v1.1.0 -m "Release v1.1.0"
   git push origin v1.1.0
   ```
4. GitHub Actions automatically runs the full unit/UI test suite, builds both `.apk` and `.aab`, writes `Vesper-v1.1.0-checksums.txt`, and publishes the GitHub Release.

### Verifying Downloaded Release Checksums

```bash
sha256sum -c Vesper-v1.1.0-checksums.txt
```
