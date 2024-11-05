# Use a base image with Gradle 8.4 and JDK 21
FROM gradle:8.4-jdk21 as builder

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

# Build for linux64 and rename the artifact
RUN ./gradlew build -PtargetPlatform=linuxX64 && \
    mv build/bin/native/releaseExecutable/KTSynologyDDNSCloudflareMultidomain.kexe /workspace/build_output/KTSynologyDDNSCloudflareMultidomainLinuxX64.kexe

# Build for linuxArm64 and rename the artifact
RUN ./gradlew build -PtargetPlatform=linuxArm64 && \
    mv build/bin/native/releaseExecutable/KTSynologyDDNSCloudflareMultidomain.kexe /workspace/build_output/KTSynologyDDNSCloudflareMultidomainLinuxArm64.kexe