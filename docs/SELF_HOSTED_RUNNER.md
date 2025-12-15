# Self-Hosted GitHub Actions Runner Setup

This guide explains how to set up a self-hosted GitHub Actions runner for the awtea repository, with a focus on containerized deployment on Linux.

## Table of Contents

1. [Why Self-Hosted Runners?](#why-self-hosted-runners)
2. [Prerequisites](#prerequisites)
3. [Quick Start with Docker](#quick-start-with-docker)
4. [Manual Installation on Linux](#manual-installation-on-linux)
5. [Container-Based Setup (Recommended)](#container-based-setup-recommended)
6. [Configuration for awtea Project](#configuration-for-awtea-project)
7. [Security Considerations](#security-considerations)
8. [Monitoring and Maintenance](#monitoring-and-maintenance)
9. [Troubleshooting](#troubleshooting)

## Why Self-Hosted Runners?

Self-hosted runners are beneficial when:

- **Private repositories** need to reduce GitHub Actions costs
- **Long-running builds** exceed GitHub's time limits (6 hours per job)
- **Custom hardware** is required (specific GPUs, architectures, etc.)
- **Network isolation** is needed for security/compliance
- **Persistent caching** across builds improves performance

For the awtea project, self-hosted runners can be useful for:
- Heavy WASM compilation workloads
- Persistent Gradle/Emscripten caches
- Testing on specific hardware configurations

## Prerequisites

### Hardware Requirements

**Minimum:**
- 2 CPU cores
- 4GB RAM
- 20GB disk space

**Recommended for awtea:**
- 4+ CPU cores (for parallel Gradle builds)
- 8GB+ RAM (Emscripten compilation is memory-intensive)
- 50GB+ disk space (for caches and build artifacts)

### Software Requirements

- Linux distribution (Ubuntu 20.04/22.04, Debian 11+, or similar)
- Docker (for containerized deployment)
- Git
- Network access to github.com and api.github.com

### GitHub Permissions

- Repository admin access (to add self-hosted runners)
- Personal access token with `repo` scope (for registration)

## Quick Start with Docker

The fastest way to get a self-hosted runner up and running:

```bash
# 1. Pull the official GitHub Actions runner image
docker pull myoung34/github-runner:latest

# 2. Run the container (replace with your values)
docker run -d \
  --name github-runner-awtea \
  --restart unless-stopped \
  -e REPO_URL="https://github.com/mdbell/awtea" \
  -e RUNNER_NAME="docker-runner-1" \
  -e RUNNER_TOKEN="<YOUR_RUNNER_TOKEN>" \
  -e RUNNER_WORKDIR="/tmp/runner-work" \
  -e LABELS="linux,docker,x64" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v runner-data:/tmp/runner-work \
  myoung34/github-runner:latest
```

**Getting the RUNNER_TOKEN:**
1. Go to repository Settings → Actions → Runners
2. Click "New self-hosted runner"
3. Copy the token from the configuration command

## Manual Installation on Linux

### Step 1: Create a Dedicated User

```bash
# Create a user for the runner
sudo useradd -m -s /bin/bash github-runner
sudo usermod -aG docker github-runner  # If using Docker-in-Docker

# Switch to the runner user
sudo su - github-runner
```

### Step 2: Download and Extract the Runner

```bash
# Create a directory for the runner
mkdir actions-runner && cd actions-runner

# Download the latest runner package
curl -o actions-runner-linux-x64-2.311.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz

# Extract the installer
tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz
```

**Note:** Check [GitHub's releases page](https://github.com/actions/runner/releases) for the latest version.

### Step 3: Configure the Runner

```bash
# Configure the runner (this will prompt for repository URL and token)
./config.sh --url https://github.com/mdbell/awtea --token <YOUR_RUNNER_TOKEN>

# Add labels (optional but recommended)
./config.sh --url https://github.com/mdbell/awtea --token <YOUR_RUNNER_TOKEN> \
  --labels linux,self-hosted,awtea,x64
```

### Step 4: Install as a Service

```bash
# Install the service (exit from github-runner user first)
exit
sudo ./svc.sh install github-runner

# Start the service
sudo ./svc.sh start

# Check status
sudo ./svc.sh status
```

### Step 5: Verify Installation

```bash
# Check runner logs
sudo journalctl -u actions.runner.mdbell-awtea.*.service -f
```

Go to repository Settings → Actions → Runners to verify the runner appears as "Idle".

## Container-Based Setup (Recommended)

For production deployments, use Docker Compose for better management:

### Docker Compose Configuration

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  github-runner:
    image: myoung34/github-runner:latest
    container_name: github-runner-awtea
    restart: unless-stopped
    
    environment:
      # Repository configuration
      REPO_URL: "https://github.com/mdbell/awtea"
      RUNNER_NAME: "docker-runner-${HOSTNAME}"
      RUNNER_TOKEN: "${RUNNER_TOKEN}"  # Set via .env file
      RUNNER_WORKDIR: "/tmp/runner-work"
      
      # Runner labels
      LABELS: "linux,docker,x64,self-hosted"
      
      # Runner group (optional)
      RUNNER_GROUP: "Default"
      
      # Ephemeral runner (remove after each job)
      EPHEMERAL: "false"
      
      # Disable automatic updates (manage via container updates)
      DISABLE_AUTO_UPDATE: "true"
    
    volumes:
      # Docker socket for Docker-in-Docker
      - /var/run/docker.sock:/var/run/docker.sock
      
      # Persistent workspace
      - runner-work:/tmp/runner-work
      
      # Cache directories for faster builds
      - gradle-cache:/home/runner/.gradle
      - emsdk-cache:/home/runner/emsdk-cache
      - deno-cache:/home/runner/.cache/deno
    
    # Resource limits
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 8G
        reservations:
          cpus: '2'
          memory: 4G

volumes:
  runner-work:
  gradle-cache:
  emsdk-cache:
  deno-cache:
```

Create `.env` file (never commit this):

```bash
RUNNER_TOKEN=your_runner_token_here
```

### Deploy with Docker Compose

```bash
# Start the runner
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the runner
docker-compose down

# Update the runner
docker-compose pull
docker-compose up -d
```

## Configuration for awtea Project

### Required Dependencies in Container

The awtea project needs these tools:

1. **Java 11** - Primary build tool
2. **Gradle** - Build system (wrapper included)
3. **Deno** - TypeScript test runner
4. **Emscripten SDK** - For WASM compilation
5. **Docker** - For WASM builds (if using Docker-based Emscripten)

### Custom Dockerfile for awtea

For full control, create a custom runner image:

```dockerfile
FROM myoung34/github-runner:latest

# Install Java 11
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    build-essential \
    cmake \
    clang \
    && rm -rf /var/lib/apt/lists/*

# Install Deno
RUN curl -fsSL https://deno.land/install.sh | sh
ENV DENO_INSTALL="/root/.deno"
ENV PATH="${DENO_INSTALL}/bin:${PATH}"

# Pre-install Emscripten SDK
RUN git clone https://github.com/emscripten-core/emsdk.git /opt/emsdk && \
    cd /opt/emsdk && \
    ./emsdk install 3.1.51 && \
    ./emsdk activate 3.1.51

# Set up Emscripten environment
ENV EMSDK="/opt/emsdk"
ENV PATH="${EMSDK}:${EMSDK}/upstream/emscripten:${PATH}"

# Pre-warm Gradle cache (optional)
RUN mkdir -p /root/.gradle

WORKDIR /tmp/runner-work
```

Build and use:

```bash
# Build the custom image
docker build -t awtea-runner:latest .

# Update docker-compose.yml to use custom image
# Change: image: myoung34/github-runner:latest
# To:     image: awtea-runner:latest

# Deploy
docker-compose up -d
```

### Workflow Configuration

Update `.github/workflows/ci.yml` to support self-hosted runners:

```yaml
jobs:
  build:
    # Use self-hosted runner with specific labels
    runs-on: [self-hosted, linux, awtea]
    
    # Or fallback to GitHub-hosted if self-hosted unavailable
    # runs-on: ${{ github.repository == 'mdbell/awtea' && 'ubuntu-latest' || 'self-hosted' }}
    
    steps:
      # ... existing steps
```

**Note:** The current CI workflow uses `ubuntu-latest` which works with both GitHub-hosted and self-hosted runners.

## Security Considerations

### Important Security Notes

⚠️ **Self-hosted runners have security implications:**

1. **Public Repositories:** 
   - Do NOT use self-hosted runners for public repos with PR builds
   - Anyone can submit a malicious PR that runs arbitrary code
   - Use self-hosted runners ONLY for protected branches

2. **Private Repositories:**
   - Self-hosted runners are safer for private repos
   - Still isolate runners from sensitive systems
   - Use separate runners for different trust levels

### Recommended Security Practices

#### 1. Network Isolation

```bash
# Create a dedicated Docker network
docker network create --driver bridge github-runner-network

# Add to docker-compose.yml
networks:
  default:
    name: github-runner-network
    external: true
```

#### 2. Use Ephemeral Runners

Ephemeral runners are destroyed after each job:

```yaml
environment:
  EPHEMERAL: "true"  # Runner is removed after each job
```

**Pros:**
- Clean state for each job
- No persistent malicious code
- Reduced attack surface

**Cons:**
- Slower (re-registration overhead)
- No persistent caches

#### 3. Restrict Runner Access

```bash
# Use repository-level runners (not organization-level)
./config.sh --url https://github.com/mdbell/awtea --token <TOKEN>

# Create a separate runner for protected branches
./config.sh --url https://github.com/mdbell/awtea --token <TOKEN> \
  --labels protected,main-only
```

#### 4. Resource Limits

Prevent resource exhaustion:

```yaml
# In docker-compose.yml
deploy:
  resources:
    limits:
      cpus: '4'          # Max 4 CPUs
      memory: 8G         # Max 8GB RAM
      pids: 1024         # Limit processes
```

#### 5. Firewall Configuration

```bash
# Allow only necessary outbound connections
sudo ufw allow out to 140.82.112.0/20  # github.com
sudo ufw allow out to 185.199.108.0/22  # GitHub Pages
sudo ufw allow out 443/tcp               # HTTPS
sudo ufw default deny outgoing           # Block everything else
```

#### 6. Regular Updates

```bash
# Update runner weekly
docker-compose pull
docker-compose up -d

# Monitor for security advisories
# https://github.com/actions/runner/security/advisories
```

### For awtea Project

**Recommended approach:**

1. Use GitHub-hosted runners for PRs (current setup)
2. Use self-hosted runners ONLY for:
   - Main branch builds
   - Scheduled nightly builds
   - Release builds

Update workflow:

```yaml
jobs:
  build:
    # GitHub-hosted for PRs
    runs-on: ubuntu-latest
  
  release-build:
    # Self-hosted for releases only
    runs-on: [self-hosted, linux, awtea]
    if: github.ref == 'refs/heads/main'
```

## Monitoring and Maintenance

### Health Checks

Monitor runner health:

```bash
# Check Docker container status
docker ps -a | grep github-runner

# Check container logs
docker logs -f github-runner-awtea

# Check resource usage
docker stats github-runner-awtea

# System-level monitoring
htop
df -h
```

### Log Management

Configure log rotation:

```yaml
# In docker-compose.yml
services:
  github-runner:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### Automated Monitoring Script

Create `monitor-runner.sh`:

```bash
#!/bin/bash

# Check if runner container is running
if ! docker ps | grep -q github-runner-awtea; then
    echo "ERROR: Runner container is not running!"
    docker-compose up -d
    exit 1
fi

# Check disk space
DISK_USAGE=$(df -h /var/lib/docker | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -gt 80 ]; then
    echo "WARNING: Disk usage is at ${DISK_USAGE}%"
    docker system prune -af --volumes
fi

# Check memory usage
MEM_USAGE=$(free | grep Mem | awk '{print int($3/$2 * 100)}')
if [ "$MEM_USAGE" -gt 90 ]; then
    echo "WARNING: Memory usage is at ${MEM_USAGE}%"
fi

echo "Runner is healthy"
```

Set up cron job:

```bash
# Edit crontab
crontab -e

# Add monitoring (every 5 minutes)
*/5 * * * * /path/to/monitor-runner.sh >> /var/log/runner-monitor.log 2>&1
```

### Backup and Restore

```bash
# Backup runner configuration
docker-compose down
tar czf runner-backup-$(date +%Y%m%d).tar.gz \
    docker-compose.yml \
    .env \
    /var/lib/docker/volumes/

# Restore
tar xzf runner-backup-*.tar.gz
docker-compose up -d
```

## Troubleshooting

### Runner Not Connecting

**Symptoms:** Runner shows as "Offline" in GitHub

**Solutions:**

1. Check network connectivity:
   ```bash
   docker exec -it github-runner-awtea curl https://api.github.com
   ```

2. Verify token is valid:
   ```bash
   # Generate a new token from GitHub and update .env
   docker-compose down
   docker-compose up -d
   ```

3. Check container logs:
   ```bash
   docker logs github-runner-awtea
   ```

### Build Failures

**Symptoms:** Jobs fail with missing dependencies

**Solutions:**

1. Check if required tools are installed:
   ```bash
   docker exec -it github-runner-awtea java -version
   docker exec -it github-runner-awtea deno --version
   docker exec -it github-runner-awtea emcc --version
   ```

2. Update container with dependencies:
   ```bash
   # Use custom Dockerfile (see above)
   docker build -t awtea-runner:latest .
   docker-compose up -d
   ```

### Out of Disk Space

**Symptoms:** Builds fail with "No space left on device"

**Solutions:**

1. Clean Docker system:
   ```bash
   docker system prune -af --volumes
   ```

2. Clear Gradle cache:
   ```bash
   docker volume rm gradle-cache
   docker-compose up -d
   ```

3. Increase disk space or add volume limits

### Performance Issues

**Symptoms:** Builds are slower than expected

**Solutions:**

1. Check resource limits:
   ```bash
   docker inspect github-runner-awtea | grep -A 10 "Resources"
   ```

2. Increase CPU/memory in docker-compose.yml

3. Enable persistent caching:
   ```yaml
   volumes:
     - gradle-cache:/home/runner/.gradle
     - emsdk-cache:/home/runner/emsdk-cache
   ```

4. Use faster storage (SSD instead of HDD)

### Container Keeps Restarting

**Symptoms:** Container restarts repeatedly

**Solutions:**

1. Check logs for errors:
   ```bash
   docker logs github-runner-awtea
   ```

2. Verify environment variables:
   ```bash
   docker exec -it github-runner-awtea env | grep RUNNER
   ```

3. Check for conflicting runners:
   ```bash
   # Remove old runner from GitHub UI
   # Repository Settings → Actions → Runners → Remove
   ```

## Advanced Topics

### Multiple Runners

Run multiple runners on the same host:

```yaml
# docker-compose-multi.yml
version: '3.8'

services:
  runner-1:
    image: awtea-runner:latest
    container_name: github-runner-1
    environment:
      RUNNER_NAME: "runner-1"
      # ... other config
  
  runner-2:
    image: awtea-runner:latest
    container_name: github-runner-2
    environment:
      RUNNER_NAME: "runner-2"
      # ... other config
```

### Auto-Scaling

Use tools like [actions-runner-controller](https://github.com/actions/actions-runner-controller) for Kubernetes-based auto-scaling.

### CI/CD for Runner Updates

Automate runner updates with a separate workflow:

```yaml
# .github/workflows/update-runner.yml
name: Update Self-Hosted Runner

on:
  schedule:
    - cron: '0 2 * * 0'  # Weekly on Sunday at 2 AM
  workflow_dispatch:

jobs:
  update-runner:
    runs-on: self-hosted
    steps:
      - name: Pull latest images
        run: docker-compose pull
      
      - name: Restart runner
        run: docker-compose up -d
```

## Cost Comparison

### GitHub-Hosted vs Self-Hosted

**GitHub-Hosted (current setup):**
- Cost: Free for public repos (2,000 min/month for private)
- Setup: Zero (instant)
- Maintenance: Zero
- Estimated usage: ~620 min/month

**Self-Hosted on AWS t3.medium:**
- Cost: ~$30/month (24/7 uptime)
- Setup: 2-4 hours initial
- Maintenance: ~1 hour/month
- Unlimited minutes

**Break-even point:** ~15,000 minutes/month for private repos

**Recommendation for awtea:**
- Stay on GitHub-hosted runners unless:
  - Moving to private repository with high usage
  - Need specialized hardware
  - Need persistent caching for 10x+ speedup

## References

- [GitHub Actions Self-Hosted Runners Documentation](https://docs.github.com/en/actions/hosting-your-own-runners)
- [Docker GitHub Runner Image](https://github.com/myoung34/docker-github-actions-runner)
- [Actions Runner Controller (Kubernetes)](https://github.com/actions/actions-runner-controller)
- [Security Hardening for Self-Hosted Runners](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)

## Getting Help

If you encounter issues:

1. Check runner logs: `docker logs -f github-runner-awtea`
2. Review GitHub Actions docs: https://docs.github.com/en/actions
3. Open an issue in the awtea repository
4. Check Docker runner issues: https://github.com/myoung34/docker-github-actions-runner/issues
