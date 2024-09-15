[![Unit tests](https://github.com/mrikirill/KTSynologyDDNSCloudflareMultidomain/actions/workflows/test.yml/badge.svg)](https://github.com/mrikirill/KTSynologyDDNSCloudflareMultidomain/actions/workflows/test.yml)

# Synology Dynamic DNS Native Agent for Cloudflare (Multidomains & Subdomains)

This project is based on my PHP version of the agent: https://github.com/mrikirill/SynologyDDNSCloudflareMultidomain

Documentation website: https://mrikirill.github.io/KTSynologyDDNSCloudflareMultidomain/

[![Sponsor](https://img.shields.io/badge/sponsor-GitHub%20Sponsors-brightgreen)](https://github.com/sponsors/mrikirill)

## Why this project?

The idea of this project is to provide a native agent for [Synology DSM](https://www.synology.com/en-global/dsm) and [SRM](https://www.synology.com/en-global/srm) devices ⚠️(read [here](#srm-support)) to update Cloudflare DNS records without requiring any dependencies.

This project is based on [the original PHP version of the agent](https://github.com/mrikirill/SynologyDDNSCloudflareMultidomain) but is written in [Kotlin Native](https://kotlinlang.org/docs/native-overview.html) and does not require the JVM. The agent is a standalone executable file that doesn't rely on system dependencies, which is the main difference from the PHP version. Additionally, it includes unit tests covering the main logic of the agent.

## Stack

- [Kotlin Native](https://kotlinlang.org/docs/native-overview.html) with target [linuxX64 and linuxArm64](https://kotlinlang.org/docs/native-target-support.html#tier-2)
- [Ktor Client Curl Engine](https://ktor.io/docs/client-engines.html#curl)

## Table of contents

* [What this native agent does](#what-this-native-agent-does)
* [SRM Support](#srm-support)
* [Build the agent locally](#build-the-agent-locally)
* [Before you start](#before-you-start)
* [How to install](#how-to-install)
* [Troubleshooting and known issues](#troubleshooting-and-known-issues)
  + [CloudFlare API free domains limitation](#cloudflare-api-free-domains-limitation)
  + [Connection test failed or error returned](#connection-test-failed-or-error-returned)
  + [Cloudflare no longer listed as a DDNS provider after a DSM update](#cloudflare-no-longer-listed-as-a-ddns-provider-after-dsm-or-srm-updates)
* [Default Cloudflare ports](#default-cloudflare-ports)
* [Debug script](#debug)
* [Credits](#credits)
* [Support this project](#support-this-project)


## What this native agent does

* Works as a standalone executable file.
* Functionality remains consistent with the [PHP version of the agent](https://github.com/mrikirill/SynologyDDNSCloudflareMultidomain).
* Designed for Synology DSM and ⚠️[Synology SRM devices](#srm-support) to integrate Cloudflare support into `Network Centre > Dynamic DNS (DDNS)`.
* Supports single domains, multidomains, subdomains, regional domains, or any combination etc (e.g., dev.my.domain.com.au, domain.com.uk, etc.).
* Simple installation process.
* Based on [CloudFlare API v4](https://developers.cloudflare.com/api)
* Use [ipify.org](https://www.ipify.org) to detect IPv6
* Compatible with both IPv4 and IPv6 dual stack.

## SRM Support
[SRM-based devices](#https://www.synology.com/en-global/products/routers) use the Linux Arm64 architecture. The agent has a build target for Linux Arm64 and should work on SRM devices. However, it needs to be built locally and tested on SRM devices. Currently, it has not been tested on SRM devices, and there is no established build process for Linux Arm64.

## Build the agent locally

1. Kotlin Native Documentation [here](https://kotlinlang.org/docs/native-get-started.html)

2. Clone the repository

3. Run the following command to build the agent

```
./gradlew build
```

Note: cause the agent includes the Ktor Client Curl Engine it requires extra steps documented [here](https://ktor.io/docs/client-engines.html#curl)

## Before you start

Before starting the installation process, make sure you have (and know) the following information, or have completed these steps:

 1. *Cloudflare credentials:*
 
	 a. Know your Cloudflare account username (or [register for an account if you're new to Cloudflare](https://dash.cloudflare.com/sign-up)); and
	 
	 b. Have your [API key](https://dash.cloudflare.com/profile/api-tokens) - no need to use your Global API key! (More info: [API keys](https://support.cloudflare.com/hc/en-us/articles/200167836-Managing-API-Tokens-and-Keys)).

	![image](/docs/example4.png)


	 c. Create a API key with following (3) permissions:
	 
	 **Zone** > **Zone.Settings** > **Read**  
	 **Zone** > **Zone** > **Read**  
	 **Zone** > **DNS** > **Edit**  

	 The affected zone ressouces have to be (at least):

	**Include** > **All zones from an account** > `<domain>`  

 2. *DNS settings:*
 
	Ensure the DNS A record(s) for the domain/zone(s) you wish to update with this script have been created (More information: [Managing DNS records](https://support.cloudflare.com/hc/en-us/articles/360019093151-Managing-DNS-records-in-Cloudflare)).

	Your DNS records should appear (or already be setup as follows) in Cloudflare:
	
	(Note: Having Proxied turned on for your A records isn't necessary, but it will prevent those snooping around from easily finding out your current IP address)

	![image](/docs/example1.png)
	
3. *SSH access to your Synology device:*

If you haven't setup this access, see the following Synology Knowledge Base article:
[How can I sign in to DSM/SRM with root privilege via SSH?[(https://kb.synology.com/en-id/DSM/tutorial/How_to_login_to_DSM_with_root_permission_via_SSH_Telnet)


## How to install

1. **SSH with sudo on your supported device:**

	 a. For DSM Users:
	 
	 Navigate to __Control Panel > Terminal & SNMP > Enable SSH service__
	 
	 b. For SRM users:
	 
	 Navigate to __Control Panel > Services > System Services > Terminal > Enable SSH service__
	 
	![image](/docs/example2.png)

2. **Connect via SSH:** Connect to your supported device via SSH and run this command:

  ```
  wget https://raw.githubusercontent.com/mrikirill/KTSynologyDDNSCloudflareMultidomain/master/install.sh -O install.sh && sudo bash install.sh
  ```


3. **Update your DDNS settings:** 

	 a. *For DSM Users:* Navigate to __Control Panel > External Access > DDNS__ then add new DDNS
	 
	 b. *For SRM users:* Navigate to __Network Centre > Internet > QuickConnect & DDNS > DDNS__ and press the Add button:

	Add/Update the DDNS settings screen as follows:

	* Service provider: Select Cloudflare
    * Hostname: this field is not used anymore, you can put any value here
	* Username:
For a single domain: __mydomain.com__
For multiple domains: __subdomain.mydomain.com|vpn.mydomain.com__
	  (ensure each domain is separated: `|`)
    
        __Note: there is 256 symbols limit on Hostname input__
	* Password: Your created Cloudflare API Key

	![image](/docs/example3.png)

	Finally, press the test connection button to confirm all information is correctly entered, before pressing Ok to save and confirm your details.

4. Enjoy 🍺 and __don't forget to deactivate SSH (step 1) if you don't need it__.

## Troubleshooting and known issues

### CloudFlare API free domains limitation

CloudFlare API doesn't support domains with a .cf, .ga, .gq, .ml, or .tk TLD (top-level domain)

For more details read here: https://github.com/mrikirill/SynologyDDNSCloudflareMultidomain/issues/28 and https://community.cloudflare.com/t/unable-to-update-ddns-using-api-for-some-tlds/167228/61

Response example:

```
{
  "result": null,
  "success": false,
  "errors": [
    {
      "code": 1038,
      "message": "You cannot use this API for domains with a .cf, .ga, .gq, .ml, or .tk TLD (top-level domain). To configure the DNS settings for this domain, use the Cloudflare Dashboard."
    }
  ],
  "messages": []
}
```

### Connection test failed or error returned

This will manifest as either 1020 error; or the update attempt not showing in your Cloudflare Audit logs.

That generally means you may not have entered something correctly in the DDNS screen for your domain(s).

Revisit [Before you begin](#before-you-begin) to ensure you have all the right information, then go back to Step 4 in [How to install](#how-to-install) to make sure everything is correctly entered.

**Handy hint:** You can also check your Cloudflare Audit logs to see what - if anything - has made it there with your API key (More information: [Understanding Cloudflare Audit Logs](https://support.cloudflare.com/hc/en-us/articles/115002833612-Understanding-Cloudflare-Audit-Logs)). Updates using the API will appear in the Audit logs as a Rec Set action.

### Cloudflare no longer listed as a DDNS provider after DSM or SRM updates

If this occurs, simply [repeat the How to install steps](#how-to-install) shown above.

## Default Cloudflare ports
Source [Identifying network ports compatible with Cloudflare's proxy](https://support.cloudflare.com/hc/en-us/articles/200169156-Identifying-network-ports-compatible-with-Cloudflare-s-proxy)

| HTTP ports supported by Cloudflare | HTTPS ports supported by Cloudflare |
|------------------------------------|-------------------------------------|
| 80                                 | 443                                 |
| 8080                               | 2053                                | 
| 8880                               | 2083                                |
| 2052                               | 2087                                | 
| 2082                               | 2096                                |
| 2086                               | 8443                                | 
| 2095                               |                                     |

## Debug

You can run this script directly to see output logs

* SSH into your Synology system 

* Run this command: 

```
./KTSynologyDDNSCloudflareMultidomain.kexe "domain1.com|vpn.domain2.com" "your-Cloudflare-token" "any" "1.2.3.4 - ipv4 address"
```

* Check output logs

## Credits

[MKDoc - generate documentation](https://www.mkdocs.org)

## Support this project

If you find this project helpful, please support it here [![Sponsor](https://img.shields.io/badge/sponsor-GitHub%20Sponsors-brightgreen)](https://github.com/sponsors/mrikirill)
