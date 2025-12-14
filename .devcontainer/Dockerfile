# AWTea Development Container
# Includes Java 11, Emscripten SDK, and Deno for complete development environment

FROM debian:bullseye-slim

# Install base dependencies
RUN apt-get update && apt-get install -y \
    git \
    curl \
    wget \
    unzip \
    python3 \
    python3-pip \
    build-essential \
    cmake \
    ca-certificates \
    sudo \
    && rm -rf /var/lib/apt/lists/*

# Install zip/unzip for SDKMAN
RUN apt-get update && apt-get install -y \
    zip \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Install SDKMAN as root
RUN curl -s "https://get.sdkman.io" | bash

# Install Java 11 via SDKMAN
RUN bash -c "source /root/.sdkman/bin/sdkman-init.sh && \
    sdk install java 11.0.22-tem && \
    sdk default java 11.0.22-tem"

# Set Java environment variables
ENV SDKMAN_DIR="/root/.sdkman"
ENV JAVA_HOME="/root/.sdkman/candidates/java/current"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install Emscripten SDK
ENV EMSDK_VERSION=3.1.51
RUN git clone https://github.com/emscripten-core/emsdk.git /opt/emsdk && \
    cd /opt/emsdk && \
    ./emsdk install ${EMSDK_VERSION} && \
    ./emsdk activate ${EMSDK_VERSION}

# Set Emscripten environment variables
ENV EMSDK=/opt/emsdk
ENV EM_CONFIG=/opt/emsdk/.emscripten
ENV PATH="${EMSDK}:${EMSDK}/upstream/emscripten:${PATH}"

# Install Deno to /usr/local/bin
ENV DENO_VERSION=v2.1.2
RUN curl -fsSL https://deno.land/install.sh | sh -s ${DENO_VERSION} && \
    mv /root/.deno/bin/deno /usr/local/bin/deno && \
    rm -rf /root/.deno

# Create vscode user for devcontainer
RUN useradd -m -s /bin/bash -u 1000 vscode && \
    echo "vscode ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# Install SDKMAN and Java for vscode user
USER vscode
RUN curl -s "https://get.sdkman.io" | bash && \
    bash -c "source /home/vscode/.sdkman/bin/sdkman-init.sh && \
    sdk install java 11.0.22-tem && \
    sdk default java 11.0.22-tem"

# Set up environment for vscode user
ENV SDKMAN_DIR="/home/vscode/.sdkman"
ENV JAVA_HOME="/home/vscode/.sdkman/candidates/java/current"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Set up Emscripten and SDKMAN for vscode user's shell
RUN echo "source /opt/emsdk/emsdk_env.sh" >> /home/vscode/.bashrc && \
    echo "source /home/vscode/.sdkman/bin/sdkman-init.sh" >> /home/vscode/.bashrc

USER root

# Install Docker CLI (for running docker commands via mounted socket)
RUN apt-get update && apt-get install -y \
    docker.io \
    && rm -rf /var/lib/apt/lists/*

# Add vscode user to docker group
RUN usermod -aG docker vscode

WORKDIR /workspace

USER vscode
