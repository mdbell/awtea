# AWTea Development Container
# Includes Java 11, Emscripten SDK, and Deno for complete development environment

FROM debian:bullseye-slim

# Install base dependencies and Docker CLI
RUN apt-get update && apt-get install -y \
    git \
    curl \
    wget \
    zip \
    unzip \
    python3 \
    python3-pip \
    build-essential \
    cmake \
    ca-certificates \
    sudo \
    docker.io \
    && rm -rf /var/lib/apt/lists/*

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
ENV DENO_INSTALL=/usr/local
RUN curl -fsSL https://deno.land/install.sh | sh -s ${DENO_VERSION}

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

# Add vscode user to docker group
RUN usermod -aG docker vscode

WORKDIR /workspace

USER vscode
