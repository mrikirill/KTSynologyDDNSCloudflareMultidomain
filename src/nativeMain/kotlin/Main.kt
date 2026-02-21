import config.Config
import config.EMBEDDED_CA_BUNDLE
import data.CloudflareServiceImpl
import data.IpifyServiceImpl
import domain.CloudflareDDNSController
import domain.SynologyException
import domain.SynologyInput
import domain.SynologyOutput
import io.ktor.client.*
import io.ktor.client.engine.curl.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import platform.posix.*
import kotlin.system.exitProcess

private var debugMode = false

@OptIn(ExperimentalForeignApi::class)
private fun printStderr(message: String) {
    val bytes = (message + "\n").encodeToByteArray()
    bytes.usePinned { pinned ->
        write(2, pinned.addressOf(0), bytes.size.convert())
    }
}

private fun debug(message: String) {
    if (debugMode) printStderr("[DEBUG] $message")
}

@OptIn(ExperimentalForeignApi::class)
private fun getSystemInfo(): String {
    val os = runCommand("uname -s") ?: "Unknown"
    val release = runCommand("uname -r") ?: ""
    val machine = runCommand("uname -m") ?: "Unknown"
    val arch = when (machine) {
        "x86_64", "amd64" -> "x86_64"
        "aarch64", "arm64" -> "ARM64"
        "armv7l" -> "ARM32"
        else -> machine
    }
    return "$os $release ($arch)"
}

@OptIn(ExperimentalForeignApi::class)
private fun runCommand(command: String): String? {
    val fp = popen(command, "r") ?: return null
    val buffer = ByteArray(256)
    val result = buffer.usePinned { pinned ->
        val read = fgets(pinned.addressOf(0), buffer.size, fp)
        read?.toKString()?.trim()
    }
    pclose(fp)
    return result
}

/**
 * Writes the embedded Mozilla CA bundle to a temporary file.
 * Returns the path to the temp file, or null if writing fails.
 */
@OptIn(ExperimentalForeignApi::class)
private fun writeEmbeddedCaBundleToTempFile(): String? {
    val tmpDir = getenv("TMPDIR")?.toKString() ?: "/tmp"
    val path = "$tmpDir/ktor_cacert.pem"
    val fd = fopen(path, "w") ?: run {
        debug("Failed to open $path for writing")
        return null
    }
    try {
        val bytes = EMBEDDED_CA_BUNDLE.encodeToByteArray()
        bytes.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1u.convert(), bytes.size.convert(), fd)
        }
        debug("Embedded CA bundle written to $path (${bytes.size} bytes)")
    } finally {
        fclose(fd)
    }
    return path
}

/**
 * From /etc.defaults/ddns_provider.conf
 * Input:
 *    1. DynDNS style request:
 *       modulepath = DynDNS
 *       queryurl = [Update URL]?[Query Parameters]
 *
 *    2. Self-defined module:
 *       modulepath = /sbin/xxxddns
 *       queryurl = DDNS_Provider_Name
 *
 *       Our service will assign parameters in the following order when calling module:
 *           ($1=username, $2=password, $3=hostname, $4=ip)
 *
 * Output:
 *    When you write your own module, you can use the following words to tell user what happen by print it.
 *    You can use your own message, but there is no multiple-language support.
 *
 *       good -  Update successfully.
 *       nochg - Update successfully but the IP address have not changed.
 *       nohost - The hostname specified does not exist in this user account.
 *       abuse - The hostname specified is blocked for update abuse.
 *       notfqdn - The hostname specified is not a fully-qualified domain name.
 *       badauth - Authenticate failed.
 *       911 - There is a problem or scheduled maintenance on provider side
 *       badagent - The user agent sent bad request(like HTTP method/parameters is not permitted)
 *       badresolv - Failed to connect to  because failed to resolve provider address.
 *       badconn - Failed to connect to provider because connection timeout.
 *
 * Debug mode:
 *       Append --debug as the last argument to enable verbose diagnostic output to stderr.
 *       Example: ./KTSynologyDDNSCloudflareMultidomain.kexe "hostname" "token" "any" "ip" --debug
 */

fun main(args: Array<String>) = runBlocking {
    debugMode = args.any { it == "--debug" }
    val filteredArgs = args.filter { it != "--debug" }

    debug("Debug mode enabled")
    debug("System: ${getSystemInfo()}")
    debug("Arguments (${filteredArgs.size}): [${filteredArgs.mapIndexed { i, a ->
        if (i == 1) "****" else a
    }.joinToString(", ")}]")

    val httpClient = HttpClient(Curl) {
        expectSuccess = true
        engine {
            val embeddedPath = writeEmbeddedCaBundleToTempFile()
            if (embeddedPath != null) {
                caInfo = embeddedPath
                debug("CA bundle: $embeddedPath")
            } else {
                debug("WARNING: Failed to write CA bundle to temp file, TLS verification may fail")
            }
        }
        headers {
            append(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = Config.REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = Config.CONNECT_TIMEOUT_MILLIS
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    try {
        val ipifyService = IpifyServiceImpl(httpClient)

        val synologyInput = if (filteredArgs.size >= 4) {
            SynologyInput(
                cloudflareApiKey = filteredArgs[1],
                hostnameList = filteredArgs[0], // we use the username field to pass the hostname list
                ip = filteredArgs[3], // synology passes the ipv4 address
            )
        } else if (filteredArgs.size == 2) {
            SynologyInput(
                cloudflareApiKey = filteredArgs[1],
                hostnameList = filteredArgs[0],
                ip = try {
                    debug("Fetching IPv4 address from ipify...")
                    val ip = ipifyService.getIpV4().ip
                    debug("IPv4 address: $ip")
                    ip
                } catch (e: Exception) {
                    debug("Failed to fetch IPv4: ${e::class.simpleName} - ${e.message}")
                    println(SynologyOutput.BAD_CONN)
                    exitProcess(0)
                }
            )
        } else {
            debug("Invalid number of arguments: ${filteredArgs.size}")
            println(SynologyOutput.BAD_PARAMS)
            exitProcess(0)
        }

        debug("Hostnames: ${synologyInput.hostnameList}")
        debug("IPv4: ${synologyInput.ip}")

        val ipv6 = try {
            debug("Fetching IPv6 address from ipify...")
            val ip = ipifyService.getIpV6().ip
            debug("IPv6 address: $ip")
            ip
        } catch (e: Exception) {
            debug("IPv6 not available: ${e::class.simpleName} - ${e.message}")
            null
        }

        val controller = CloudflareDDNSController(
            cloudflareService = CloudflareServiceImpl(
                httpClient = httpClient,
                cloudflareApiKey = synologyInput.cloudflareApiKey
            ),
            ipv4 = synologyInput.ip,
            ipv6 = ipv6,
            hostnameList = synologyInput.hostnameList
        )

        try {
            debug("Step 1/4: Verifying Cloudflare API token...")
            controller.verifyToken()
            debug("Step 1/4: Token verified successfully")

            debug("Step 2/4: Matching hostnames with Cloudflare zones...")
            controller.matchHostnamesWithZones()
            debug("Step 2/4: Matched ${controller.getDnsRecordListRequest().size} DNS record request(s)")

            debug("Step 3/4: Fetching existing DNS records...")
            controller.setDnsRecords()
            debug("Step 3/4: Found ${controller.getDnsRecordList().size} DNS record(s) to update")

            debug("Step 4/4: Updating DNS records...")
            controller.updateDnsRecords()
        } catch (e: SynologyException) {
            e.detail?.let { debug(it) }
            println(e.message)
        } catch (e: Exception) {
            debug("Unexpected error: ${e::class.simpleName} - ${e.message}")
            if (e.message?.contains("SSL") == true || e.message?.contains("certificate") == true) {
                debug("TLS verification failed. Hint: Ensure CA certificates are installed (e.g., ca-certificates package)")
                println(SynologyOutput.BAD_CONN)
            } else {
                println(SynologyOutput.UNKNOWN_ERROR)
            }
        }
    } finally {
        httpClient.close()
    }
}
