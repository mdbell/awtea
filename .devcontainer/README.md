# AWTea Development Container

This directory contains the Visual Studio Code Dev Container configuration for AWTea development.

## Overview

The devcontainer provides a consistent, reproducible development environment with all necessary tools pre-installed:

- **Java 11** (via SDKMAN, allowing easy version switching)
- **Emscripten SDK** (for compiling C code to WebAssembly - no Docker required!)
- **Deno** (for TypeScript/WASM tests)
- **Gradle** (via wrapper)
- **Git** and other essential development tools

## Usage

### Prerequisites

- [Docker](https://www.docker.com/products/docker-desktop) installed and running
- [Visual Studio Code](https://code.visualstudio.com/) with the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

### Opening the Project in a Dev Container

1. Clone the repository:
   ```bash
   git clone https://github.com/mdbell/awtea.git
   cd awtea
   ```

2. Open in VS Code:
   ```bash
   code .
   ```

3. When prompted, click **"Reopen in Container"** (or run the command `Dev Containers: Reopen in Container` from the Command Palette)

4. Wait for the container to build and start (first time may take a few minutes)

5. Once ready, you can use the integrated terminal to run commands:
   ```bash
   ./gradlew build
   ./gradlew :awtea-graphics:buildAwtRasterWasm
   ./gradlew :awtea-graphics:denoTest
   ```

## Features

### Installed Tools

- **Java Development Kit (JDK)**: Java 11 managed via SDKMAN
- **SDKMAN**: SDK manager for easy JDK version switching
- **Emscripten SDK**: Version 3.1.51 for WebAssembly compilation (directly callable with `emcc`)
- **Deno**: Version 2.1.2 installed to /usr/local/bin for TypeScript/WASM tests
- **Gradle**: Build automation via the Gradle wrapper (`./gradlew`)
- **VS Code Extensions**: Pre-configured extensions for Java, Gradle, and Deno development

### Configuration Highlights

- **Custom Dockerfile**: Builds a complete development environment with all tools
- **SDKMAN Integration**: Allows switching JDK versions using `sdk` commands
- **Emscripten SDK**: Pre-installed and configured - call `emcc` directly, no Docker needed!
- **Deno Integration**: Enabled only for `./awtea-graphics/src/test/deno` directory
- **Port Forwarding**: Ports 8080 and 3000 forwarded for web application development

## CI/CD Reusability

This devcontainer can be reused for CI/CD pipelines in several ways:

### GitHub Actions

The same base image and features can be referenced in GitHub Actions workflows. 

**Recommended Approach (using host Docker):**

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '11'
          cache: 'gradle'
      
      - name: Setup Deno
        uses: denoland/setup-deno@v1
        with:
          deno-version: v2.x
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Build
        run: ./gradlew build
      
      - name: Test WASM
        run: ./gradlew :awtea-graphics:denoTest
```

**Alternative Approach (using container with privileged mode):**

```yaml
name: Build and Test (Container)

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    # Note: --privileged grants extensive permissions
    container:
      image: mcr.microsoft.com/devcontainers/java:1-11-bullseye
      options: --privileged
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Deno
        uses: denoland/setup-deno@v1
        with:
          deno-version: v2.x
      
      - name: Build
        run: ./gradlew build
      
      - name: Test WASM
        run: ./gradlew :awtea-graphics:denoTest
```

> **Note:** Keep the versions in sync with the devcontainer configuration and the existing CI workflows in `.github/workflows/`.

> **Security Note:** The first approach (using host Docker with setup actions) is more secure than using `--privileged` containers. The devcontainer uses Docker socket mounting for development convenience, but CI/CD should prefer the setup-action approach when possible.

### Docker Compose

For more complex CI setups, you can use Docker Compose:

```yaml
version: '3.8'
services:
  awtea-ci:
    image: mcr.microsoft.com/devcontainers/java:1-11-bullseye
    volumes:
      - .:/workspace
      - /var/run/docker.sock:/var/run/docker.sock
    working_dir: /workspace
    command: ./gradlew build
```

### Dockerfile for CI

> **Security Note:** The example below shows a typical CI Dockerfile but includes piping a remote script to shell, which has security implications. Consider these safer alternatives:
> - Use the official Deno Docker image: `denoland/deno:alpine`
> - Use a pre-built image that includes both Java and Deno
> - In GitHub Actions, use the `denoland/setup-deno@v1` action instead

You can create a CI-specific Dockerfile based on the devcontainer configuration:

```dockerfile
FROM mcr.microsoft.com/devcontainers/java:1-11-bullseye

# Install Docker
RUN apt-get update && \
    apt-get install -y docker.io && \
    apt-get clean

# Install Deno using official installation
# For better security, verify the script or use package managers when available
RUN curl -fsSL https://deno.land/install.sh | sh
ENV PATH="${PATH}:/root/.deno/bin"

WORKDIR /workspace
```

**Safer alternative using multi-stage build with official images:**

```dockerfile
# Use official Deno image for Deno installation
FROM denoland/deno:alpine-1.40.0 AS deno

# Main image
FROM mcr.microsoft.com/devcontainers/java:1-11-bullseye

# Install Docker
RUN apt-get update && \
    apt-get install -y docker.io && \
    apt-get clean

# Copy Deno from official image
COPY --from=deno /usr/bin/deno /usr/local/bin/deno

WORKDIR /workspace
```

## Development Workflow

### Managing Java Versions with SDKMAN

The devcontainer includes SDKMAN for easy Java version management:

```bash
# List available Java versions
sdk list java

# Install a different Java version (e.g., Java 17)
sdk install java 17.0.10-tem

# Switch to a different installed version
sdk use java 17.0.10-tem

# Set a version as default
sdk default java 17.0.10-tem

# Show current Java version
sdk current java
```

### Building the Project

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :awtea-graphics:build

# Clean build
./gradlew clean build
```

### WASM Development

```bash
# Build WASM module
./gradlew :awtea-graphics:buildAwtRasterWasm

# Run Deno tests
./gradlew :awtea-graphics:denoTest

# Run Deno tests directly
cd awtea-graphics/src/test/deno
deno test --allow-read
```

### Running Examples

```bash
# Build hello-world example
./gradlew :examples:hello-world:build

# The built files will be in examples/hello-world/build/dist/
# Serve them with any web server, e.g.:
cd examples/hello-world/build/dist
python3 -m http.server 8080
# Then open http://localhost:8080 in your browser
```

### Code Generation

```bash
# Generate enum definitions from YAML schemas
./gradlew generateEnums

# Generate API coverage documentation
./gradlew generateDocs
```

## Troubleshooting

### Docker Socket Permission Issues

If you encounter permission errors with Docker:

```bash
# Inside the container
sudo chmod 666 /var/run/docker.sock
```

### Gradle Daemon Issues

If Gradle becomes unresponsive:

```bash
./gradlew --stop
```

### Deno Cache Issues

If Deno modules fail to load:

```bash
cd awtea-graphics/src/test/deno
deno cache --reload *.ts
```

## Customization

### Adding More Tools

To add additional tools or features, edit `.devcontainer/devcontainer.json`:

```json
{
  "features": {
    "ghcr.io/devcontainers/features/node:1": {
      "version": "20"
    }
  }
}
```

### Changing Java Version

To use a different Java version, change the base image:

```json
{
  "image": "mcr.microsoft.com/devcontainers/java:1-17-bullseye"
}
```

Available Java versions include `1-11-bullseye`, `1-17-bullseye`, and `1-21-bullseye`.

## References

- [VS Code Dev Containers Documentation](https://code.visualstudio.com/docs/devcontainers/containers)
- [Dev Container Features](https://containers.dev/features)
- [AWTea Project Documentation](../README.md)
