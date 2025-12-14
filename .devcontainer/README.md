# AWTea Development Container

This directory contains the Visual Studio Code Dev Container configuration for AWTea development.

## Overview

The devcontainer provides a consistent, reproducible development environment with all necessary tools pre-installed:

- **Java 11** (Microsoft OpenJDK, required for the project)
- **Gradle** (via wrapper)
- **Docker-in-Docker** (for Emscripten/WASM compilation)
- **Deno** (for TypeScript/WASM tests)
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

- **Java Development Kit (JDK)**: Java 11 (Microsoft OpenJDK)
- **Gradle**: Build automation via the Gradle wrapper (`./gradlew`)
- **Docker**: Docker-in-Docker support for running Emscripten containers (version 20)
- **Deno**: TypeScript/JavaScript runtime for WASM tests (v2.x)
- **VS Code Extensions**: Pre-configured extensions for Java, Gradle, Deno, and Docker development

### Configuration Highlights

- **Java Runtime**: Uses Java 11 directly (Microsoft OpenJDK from the base image)
- **Deno Integration**: Enabled only for `./awtea-graphics/src/test/deno` directory
- **Port Forwarding**: Ports 8080 and 3000 forwarded for web application development
- **Docker Socket Mounting**: Enables Docker-in-Docker for WASM compilation

## CI/CD Reusability

This devcontainer can be reused for CI/CD pipelines in several ways:

### GitHub Actions

The same base image and features can be referenced in GitHub Actions workflows. This example shows how to set up a CI workflow with the same tools as the devcontainer:

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    container:
      image: mcr.microsoft.com/devcontainers/java:1-11-bullseye
      options: --privileged  # For Docker-in-Docker
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Deno
        uses: denoland/setup-deno@v1
        with:
          deno-version: v2.x
      
      - name: Setup Docker
        run: |
          # Docker service should be available via Docker-in-Docker
          docker --version
      
      - name: Build
        run: ./gradlew build
      
      - name: Test WASM
        run: ./gradlew :awtea-graphics:denoTest
```

> **Note:** Keep the versions in sync with the devcontainer configuration and the existing CI workflows in `.github/workflows/`.

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

You can create a CI-specific Dockerfile based on the devcontainer configuration. Note that this is just an example - consider using package managers or official images in production CI systems:

```dockerfile
FROM mcr.microsoft.com/devcontainers/java:1-11-bullseye

# Install Docker
RUN apt-get update && \
    apt-get install -y docker.io && \
    apt-get clean

# Install Deno using official installation
RUN curl -fsSL https://deno.land/install.sh | sh
ENV PATH="${PATH}:/root/.deno/bin"

WORKDIR /workspace
```

> **Security Note:** For production CI systems, prefer using official GitHub Actions or package managers rather than piping remote scripts to shell.

## Development Workflow

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
