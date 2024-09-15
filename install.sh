#!/bin/bash

# Check if system is LinuxX64 or LinuxArm64
ARCH=$(uname -m)
if [[ "$ARCH" == "aarch64" ]]; then
    SYSTEM="LinuxArm64"
    echo "Installer doesn't support LinuxArm64. Please make the build manually."
    exit 1
elif [[ "$ARCH" == "x86_64" ]]; then
    SYSTEM="LinuxX64"
else
    echo "Unsupported architecture: $ARCH"
    exit 1
fi

# Construct the download URL based on the system architecture
REPO_URL="https://github.com/mrikirill/KTSynologyDDNSCloudflareMultidomain/releases/latest/download/KTSynologyDDNSCloudflareMultidomain${SYSTEM}.kexe"
DOWNLOAD_PATH="/tmp/KTSynologyDDNSCloudflareMultidomain${SYSTEM}.kexe"

# Download the latest release based on the system
echo "Downloading from $REPO_URL..."
curl -L -o "$DOWNLOAD_PATH" "$REPO_URL"

if [ $? -ne 0 ]; then
    echo "Failed to download the latest release for $SYSTEM."
    exit 1
fi

# Rename the downloaded file
TARGET_FILE="/usr/syno/bin/ddns/KTSynologyDDNSCloudflareMultidomain.kexe"
sudo mv "$DOWNLOAD_PATH" "$TARGET_FILE"

# Set permissions
sudo chmod 755 "$TARGET_FILE"

# Modify /etc.defaults/ddns_provider.conf
CONF_FILE="/etc.defaults/ddns_provider.conf"
STRING_TO_ADD="[Cloudflare]\n  modulepath=/usr/syno/bin/ddns/KTSynologyDDNSCloudflareMultidomain.kexe\n  queryurl=https://www.cloudflare.com/"

if ! grep -q "[Cloudflare]" "$CONF_FILE"; then
    echo -e "$STRING_TO_ADD" | sudo tee -a "$CONF_FILE" > /dev/null
else
    echo "Cloudflare configuration already exists."
fi

echo "Installation complete for $SYSTEM."