import config.Config
import data.CloudflareServiceImpl
import data.IpifyServiceImpl
import domian.CloudflareDDNSController
import domian.SynologyInput
import domian.SynologyOutput
import io.ktor.client.*
import io.ktor.client.engine.curl.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

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
 */

fun main(args: Array<String>) = runBlocking {
    if (args.size != 4) {
        println(SynologyOutput.BAD_PARAMS)
        exitProcess(0)
    }

    val synologyInput = SynologyInput(
        cloudflareApiKey = args[1],
        hostnameList = args[0], // we use the username field to pass the hostname list
        ip = args[3], // synology passes the ipv4 address
    )

    val httpClient = HttpClient(Curl) {
        expectSuccess = true
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

    val controller = CloudflareDDNSController(
        cloudflareService = CloudflareServiceImpl(
            httpClient = httpClient,
            cloudflareApiKey = synologyInput.cloudflareApiKey
        ),
        ipifyService = IpifyServiceImpl(
            httpClient = httpClient
        ),
        ipv4 = synologyInput.ip,
        hostnameList = synologyInput.hostnameList
    )
    controller.verifyToken()
    controller.matchHostnamesWithZones()
    controller.setDnsRecords()
    controller.updateDnsRecords()
}
