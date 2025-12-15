# CI/CD Pipeline Documentation

This document describes the CI/CD pipeline setup for the awtea project using GitHub Actions.

## Overview

The awtea project uses GitHub Actions for continuous integration and delivery. The pipeline is designed to be cost-effective, utilizing the free tier of GitHub Actions for public repositories while ensuring code quality through automated builds and tests.

## Workflows

### 1. Main CI Pipeline (`ci.yml`)

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches
- Manual workflow dispatch

**Jobs:**

#### Build and Test Job
- **Runs on:** Ubuntu (Linux) - 1x cost multiplier
- **Purpose:** Build core modules and run tests
- **Steps:**
  1. Checkout repository
  2. Set up Java 11 (with Gradle caching)
  3. Set up Deno for TypeScript tests
  4. Generate enums from YAML schemas
  5. Build core modules (excluding examples with known build issues)
  6. Run tests
  7. Archive build artifacts (JARs)
  8. Archive test results and reports

**Note:** Examples are excluded from CI builds due to known TeaVM compilation issues that are being addressed separately.

#### WASM Build Job (Main Branch Only)
- **Runs on:** Ubuntu (Linux)
- **Condition:** Only runs on pushes to `main` branch
- **Purpose:** Build the native WASM rasterizer module
- **Steps:**
  1. Set up Emscripten SDK for C → WASM compilation
  2. Build WASM module using `emcc`
  3. Archive WASM artifact for 30 days

**Cost Optimization:** This job is expensive (Emscripten setup + compilation) so it only runs on the main branch, not on every PR.

#### Deno Tests Job
- **Runs on:** Ubuntu (Linux)
- **Purpose:** Test WASM rasterizer in isolation using Deno
- **Steps:**
  1. Build WASM module
  2. Run TypeScript/Deno tests against WASM binary

### 2. WASM Rasterizer Tests (`wasm-rasterizer-tests.yml`)

**Triggers:**
- Push to `main` or `develop` with changes to WASM-related files
- Pull requests with changes to WASM-related files

**Purpose:** Focused testing of WASM rasterizer changes

This workflow is path-specific and only runs when WASM-related code changes:
- `awtea-graphics/src/main/native/**`
- `awtea-graphics/src/test/deno/**`
- `awtea-graphics/build.gradle.kts`

### 3. Copilot Setup Steps (`copilot-setup-steps.yml`)

**Purpose:** Verify the development environment setup process

Ensures that the setup steps documented for Copilot agents work correctly, including:
- Java 11 setup
- Deno installation
- Emscripten SDK
- Enum generation
- Gradle dependency download

## Cost Optimization Strategies

### Current Optimizations

1. **Linux Runners Only**: All jobs use `ubuntu-latest` (1x cost multiplier)
   - Windows runners: 2x cost
   - macOS runners: 10x cost

2. **Gradle Dependency Caching**: Uses `actions/setup-java` with `cache: 'gradle'`
   - Speeds up builds
   - Reduces network usage
   - Saves ~2-5 minutes per workflow run

3. **Emscripten SDK Caching**: Uses `actions-cache-folder: 'emsdk-cache'`
   - Caches the ~2GB Emscripten SDK between runs
   - Saves ~5-10 minutes on WASM builds

4. **Strategic Job Execution**:
   - WASM builds only on `main` branch (not every PR)
   - Path-specific triggers for WASM tests
   - Concurrency control to cancel outdated runs

5. **No-Daemon Mode**: Uses `--no-daemon` flag for Gradle
   - Reduces memory footprint
   - Prevents daemon startup/shutdown overhead in CI

6. **Artifact Retention**:
   - Build artifacts: 7 days
   - WASM artifacts: 30 days (longer retention for releases)

### Cost Monitoring

**Free Tier Limits:**
- **Public repositories:** GitHub Actions is free with usage limits
  - Linux runners have generous limits suitable for open source projects
  - Check current limits at [GitHub Actions billing documentation](https://docs.github.com/en/billing/managing-billing-for-github-actions/about-billing-for-github-actions)
- **Private repositories:** 2,000 minutes/month on free tier (Linux runners)

**Current Usage Estimate:**
- Main CI (per run): ~3-5 minutes
- WASM build (main only): ~8-12 minutes
- Deno tests: ~2-3 minutes
- Total per PR: ~5-8 minutes
- Total per main push: ~13-20 minutes

**Monitor Usage:**
1. Go to repository Settings → Billing
2. Click on "Actions" under "Plans and usage"
3. View detailed usage statistics

### Future Optimizations

If costs become a concern:

1. **Matrix Builds**: Currently not using matrix strategy. Could add if needed:
   ```yaml
   strategy:
     matrix:
       java: [11, 17]
   ```

2. **Split Testing**: Separate fast/slow tests
   - Run fast tests on all PRs
   - Run slow tests only on main or nightly

3. **Scheduled Builds**: Move expensive tasks to nightly builds
   ```yaml
   on:
     schedule:
       - cron: '0 2 * * *'  # 2 AM daily
   ```

4. **Self-Hosted Runners**: For private repos or high-volume usage
   - Zero cost (except infrastructure)
   - Full control over environment

## Artifacts

### Build Artifacts
- **Name:** `build-artifacts`
- **Contents:** All module JARs (excluding `-plain.jar` files)
- **Retention:** 7 days
- **Use:** Verify successful builds, download for testing

### Test Results
- **Name:** `test-results`
- **Contents:** JUnit XML results and HTML reports
- **Retention:** 7 days
- **Use:** Debugging test failures

### WASM Artifacts
- **Name:** `awt-raster-wasm`
- **Contents:** `awt_raster.wasm` binary
- **Retention:** 30 days
- **Use:** Testing, integration, debugging

## Troubleshooting

### Common Issues

#### 1. Gradle Build Failure
**Symptom:** Build fails with task dependency errors

**Solution:**
- Check if `generateEnums` task ran before compilation
- Ensure examples are excluded: `-x :examples:gui-demo:build`

#### 2. WASM Compilation Failure
**Symptom:** `emcc` command not found or compilation errors

**Solution:**
- Verify Emscripten version matches `3.1.51`
- Check that `emsdk-cache` action is configured correctly
- Review native C code in `awtea-graphics/src/main/native/`

#### 3. Deno Tests Fail
**Symptom:** Deno tests fail but WASM builds successfully

**Solution:**
- Check WASM module imports/exports match TypeScript expectations
- Verify test files in `awtea-graphics/src/test/deno/`
- Review Deno version compatibility

#### 4. Out of Memory Errors
**Symptom:** Gradle or compilation OOM errors

**Solution:**
- Add `org.gradle.jvmargs=-Xmx2g` to `gradle.properties`
- Use `--no-daemon` flag (already implemented)
- Split large jobs into smaller ones

#### 5. Cache Corruption
**Symptom:** Inconsistent builds, "Could not resolve" errors

**Solution:**
- Clear GitHub Actions cache (Settings → Actions → Caches)
- Run with `--refresh-dependencies` flag once
- Wait for cache to rebuild

### Debugging Workflow Runs

1. **View Logs:**
   - Go to Actions tab in GitHub
   - Click on workflow run
   - Expand failed steps to see logs

2. **Download Artifacts:**
   - Click on workflow run
   - Scroll to "Artifacts" section
   - Download and inspect

3. **Re-run Failed Jobs:**
   - Click "Re-run failed jobs" button
   - Or "Re-run all jobs" for complete re-run

4. **Local Reproduction:**
   ```bash
   # Simulate CI environment locally
   ./gradlew clean
   ./gradlew generateEnums --no-daemon
   ./gradlew build -x :examples:gui-demo:build -x :examples:hello-world:build --no-daemon
   ```

## Maintenance

### Regular Tasks

1. **Update Dependencies:**
   - Keep GitHub Actions up to date (e.g., `actions/checkout@v4`)
   - Update Emscripten SDK version as needed
   - Update Java/Deno versions for security patches
   - **Security Note:** For maximum security, consider pinning third-party actions to commit SHAs instead of tags (e.g., `uses: mymindstorm/setup-emsdk@13d8fd355c5d882f4a1c563ae62e3573322aeca4` instead of `@v14`). This prevents potential tag hijacking but requires more maintenance to track updates.

2. **Monitor Performance:**
   - Check average workflow duration monthly
   - Optimize slow steps
   - Review cache hit rates

3. **Review Artifacts:**
   - Periodically check artifact storage usage
   - Adjust retention periods if needed

### Adding New Jobs

When adding new CI jobs, consider:

1. **Cost Impact:**
   - Use Linux runners
   - Add appropriate conditions (`if:`)
   - Cache dependencies

2. **Parallelization:**
   - Jobs run in parallel by default
   - Use `needs:` to create dependencies

3. **Naming:**
   - Use descriptive job names
   - Include runner type if not obvious

Example:
```yaml
new-job:
  name: My New Job
  runs-on: ubuntu-latest
  needs: build  # Runs after build job
  if: github.ref == 'refs/heads/main'  # Conditional execution
  
  steps:
    - uses: actions/checkout@v4
    # ... steps
```

## Status Badges

The CI status badge in the README shows the current build status:

```markdown
[![CI](https://github.com/mdbell/awtea/actions/workflows/ci.yml/badge.svg)](https://github.com/mdbell/awtea/actions/workflows/ci.yml)
```

**States:**
- 🟢 **Passing:** All jobs successful
- 🔴 **Failing:** One or more jobs failed
- 🟡 **In Progress:** Workflow is running
- ⚪ **No Status:** Workflow hasn't run yet

## Self-Hosted Runners

For advanced use cases (private repositories with high usage, custom hardware requirements, or persistent caching), consider setting up self-hosted runners.

See the [Self-Hosted Runner Setup Guide](SELF_HOSTED_RUNNER.md) for detailed instructions on:
- Docker-based runner deployment
- Security best practices
- Resource optimization
- Monitoring and maintenance

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Building Java with Gradle](https://docs.github.com/en/actions/automating-builds-and-tests/building-and-testing-java-with-gradle)
- [Caching Dependencies](https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows)
- [GitHub Actions Billing](https://docs.github.com/en/billing/managing-billing-for-github-actions/about-billing-for-github-actions)
- [Self-Hosted Runners](SELF_HOSTED_RUNNER.md) - Setup guide for self-hosted runners
