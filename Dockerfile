# Use a base image with Gradle 8.11.1 and JDK 21
FROM gradle:8.11.1-jdk21 as builder
ARG TARGETARCH

# Set the working directory inside the container
WORKDIR /workspace

# Copy the entire project into the working directory
COPY . .

# Ensure that gradlew has execute permissions
RUN chmod +x ./gradlew

# Install any additional dependencies needed for the build (e.g., libcurl)
RUN apt-get update && apt-get install -y libcurl4-openssl-dev

# Create output directory for artifacts
RUN mkdir -p /workspace/build_output

# Build based on the target architecture
RUN if [ "$TARGETARCH" = "amd64" ]; then \
        ./gradlew build -PtargetPlatform=linuxX64 && \
        mv build/bin/native/releaseExecutable/KTSynologyDDNSCloudflareMultidomain.kexe /workspace/build_output/KTSynologyDDNSCloudflareMultidomainLinuxX64.kexe; \
    elif [ "$TARGETARCH" = "arm64" ]; then \
        ./gradlew build -PtargetPlatform=linuxArm64 && \
        mv build/bin/native/releaseExecutable/KTSynologyDDNSCloudflareMultidomain.kexe /workspace/build_output/KTSynologyDDNSCloudflareMultidomainLinuxArm64.kexe; \
    fi

# Export stage
FROM scratch
COPY --from=builder /workspace/build_output/ /