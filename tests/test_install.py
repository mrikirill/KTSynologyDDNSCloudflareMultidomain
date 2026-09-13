"""Exercise the installer without root, network access, or Synology hardware.

Run with: INSTALL_TEST_SHELL=dash python3 -m unittest discover -s tests -v
Only filesystem paths are redirected in a temporary copy of the installer.
"""

import os
from pathlib import Path
import shlex
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class InstallerTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.bin = self.root / "bin"
        self.bin.mkdir()
        self.target = self.root / "agent.kexe"
        self.conf = self.root / "ddns_provider.conf"
        self.original_conf = "[OtherProvider]\n  queryurl=https://example.com/\n"
        self.conf.write_text(self.original_conf)
        self.shell = shlex.split(os.environ.get("INSTALL_TEST_SHELL", "sh"))
        self.shell[0] = shutil.which(self.shell[0]) or self.shell[0]
        # No bash, real sudo, or real curl is reachable through PATH.
        for command in ("grep", "mv", "chmod", "tee", "sed"):
            (self.bin / command).symlink_to(shutil.which(command))
        self.stub("uname", 'printf "%s\\n" "$TEST_ARCH"')
        self.stub("sudo", 'exec "$@"')
        self.stub("curl", '''
if [ "$TEST_DOWNLOAD_FAIL" = 1 ]; then exit 7; fi
[ "$1" = -L ] && [ "$2" = -o ] && [ "$#" = 4 ] || exit 2
printf '%s\\n' "$4" > "$3"
''')
        source = (ROOT / "install.sh").read_text()
        source = source.replace(
            "/usr/syno/bin/ddns/KTSynologyDDNSCloudflareMultidomain.kexe",
            str(self.target),
        ).replace("/etc.defaults/ddns_provider.conf", str(self.conf))
        source = source.replace("/tmp/KTSynology", str(self.root / "KTSynology"))
        self.script = self.root / "install.sh"
        self.script.write_text(source)

    def stub(self, name, body):
        path = self.bin / name
        path.write_text("#!/bin/sh\n" + body + "\n")
        path.chmod(0o755)

    def run_installer(self, arch, download_fail=False):
        return subprocess.run(
            self.shell + [str(self.script)],
            env={**os.environ, "PATH": str(self.bin), "TEST_ARCH": arch,
                 "TEST_DOWNLOAD_FAIL": "1" if download_fail else "0"},
            capture_output=True, text=True, timeout=10,
        )

    def assert_installed(self, arch, asset):
        result = self.run_installer(arch)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(result.stderr, "")
        self.assertEqual(
            self.target.read_text(),
            "https://github.com/mrikirill/KTSynologyDDNSCloudflareMultidomain/"
            "releases/latest/download/" + asset + "\n",
        )
        self.assertEqual(self.target.stat().st_mode & 0o777, 0o755)
        self.assertEqual(
            self.conf.read_text(), self.original_conf +
            "[Cloudflare]\n  modulepath=" + str(self.target) +
            "\n  queryurl=https://www.cloudflare.com/\n",
        )

    def test_arm64_install_without_bash(self):
        self.assert_installed("aarch64", "KTSynologyDDNSCloudflareMultidomainLinuxArm64.kexe")

    def test_x64_install_without_bash(self):
        self.assert_installed("x86_64", "KTSynologyDDNSCloudflareMultidomainLinuxX64.kexe")

    def test_unsupported_architecture_leaves_system_unchanged(self):
        result = self.run_installer("armv7l")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Unsupported architecture", result.stdout)
        self.assertFalse(self.target.exists())
        self.assertEqual(self.conf.read_text(), self.original_conf)

    def test_download_failure_preserves_existing_installation(self):
        self.target.write_text("existing agent")
        result = self.run_installer("aarch64", download_fail=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Failed to download", result.stdout)
        self.assertEqual(self.target.read_text(), "existing agent")
        self.assertEqual(self.conf.read_text(), self.original_conf)
