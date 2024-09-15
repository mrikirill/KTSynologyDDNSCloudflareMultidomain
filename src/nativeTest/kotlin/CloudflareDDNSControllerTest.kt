import data.CloudflareServiceImpl
import data.IpifyServiceImpl
import data.model.DnsRecordTypeEnumDto
import domian.CloudflareDDNSController
import domian.SynologyOutput
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CloudflareDDNSControllerTest {
    private val mockEngine = MockEngine { request ->
        when (request.url.fullPath) {
            "/client/v4/user/tokens/verify" -> {
                if (request.headers[HttpHeaders.Authorization] == "Bearer cloudflareApiKey") {
                    respond(
                        content = """
                    {
                      "errors": [],
                      "messages": [],
                      "success": true,
                      "result": {
                        "expires_on": "2020-01-01T00:00:00Z",
                        "id": "ed17574386854bf78a67040be0a770b0",
                        "not_before": "2018-07-01T05:20:00Z",
                        "status": "active"
                      }
                    }
                """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    )
                }
                else {
                    respondError(HttpStatusCode.BadRequest)
                }
            }
            "/client/v4/zones?per_page=50&status=active" -> respond(
                content = """
                    {
                      "errors": [],
                      "messages": [],
                      "success": true,
                      "result_info": {
                        "count": 1,
                        "page": 1,
                        "per_page": 50,
                        "total_count": 2000
                      },
                      "result": [
                        {
                          "account": {
                            "id": "023e105f4ecef8ad9ca31a8372d0c353",
                            "name": "Example Account Name"
                          },
                          "activated_on": "2014-01-02T00:01:00.12345Z",
                          "created_on": "2014-01-01T05:20:00.12345Z",
                          "development_mode": 7200,
                          "id": "mock-zone-id",
                          "meta": {
                            "cdn_only": true,
                            "custom_certificate_quota": 1,
                            "dns_only": true,
                            "foundation_dns": true,
                            "page_rule_quota": 100,
                            "phishing_detected": false,
                            "step": 2
                          },
                          "modified_on": "2014-01-01T05:20:00.12345Z",
                          "name": "osome.dev",
                          "name_servers": [
                            "bob.ns.cloudflare.com",
                            "lola.ns.cloudflare.com"
                          ],
                          "original_dnshost": "NameCheap",
                          "original_name_servers": [
                            "ns1.originaldnshost.com",
                            "ns2.originaldnshost.com"
                          ],
                          "original_registrar": "GoDaddy",
                          "owner": {
                            "id": "023e105f4ecef8ad9ca31a8372d0c353",
                            "name": "Example Org",
                            "type": "organization"
                          },
                          "vanity_name_servers": [
                            "ns1.example.com",
                            "ns2.example.com"
                          ]
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
            "/client/v4/zones/mock-zone-id/dns_records?type=A&name=osome.dev" -> respond(
                content = """
                    {
                    "result": [
                        {
                            "id": "mock-dns-record-id",
                            "zone_id": "mock-zone-id",
                            "zone_name": "osome.dev",
                            "name": "osome.dev",
                            "type": "A",
                            "content": "1.2.3.4",
                            "proxiable": true,
                            "proxied": true,
                            "ttl": 1,
                            "meta": {
                                "auto_added": false,
                                "managed_by_apps": false,
                                "managed_by_argo_tunnel": false
                            },
                            "comment": null,
                            "tags": [],
                            "created_on": "2024-03-15T02:23:16.702904Z",
                            "modified_on": "2024-03-15T02:23:16.702904Z"
                        }
                    ],
                    "success": true,
                    "errors": [],
                    "messages": [],
                    "result_info": {
                        "page": 1,
                        "per_page": 100,
                        "count": 1,
                        "total_count": 1,
                        "total_pages": 1
                    }
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
            "/client/v4/zones/mock-zone-id/dns_records?type=AAAA&name=osome.dev" -> respond(
                content = """
                    {
                    "result": [
                        {
                            "id": "mock-dns-record-id",
                            "zone_id": "mock-zone-id",
                            "zone_name": "osome.dev",
                            "name": "osome.dev",
                            "type": "AAAA",
                            "content": "2a00:1450:400f:80d::200e",
                            "proxiable": true,
                            "proxied": true,
                            "ttl": 1,
                            "meta": {
                                "auto_added": false,
                                "managed_by_apps": false,
                                "managed_by_argo_tunnel": false
                            },
                            "comment": null,
                            "tags": [],
                            "created_on": "2024-03-15T02:23:16.702904Z",
                            "modified_on": "2024-03-15T02:23:16.702904Z"
                        }
                    ],
                    "success": true,
                    "errors": [],
                    "messages": [],
                    "result_info": {
                        "page": 1,
                        "per_page": 100,
                        "count": 1,
                        "total_count": 1,
                        "total_pages": 1
                    }
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
            "/client/v4/zones/mock-zone-id/dns_records/mock-dns-record-id" -> respond(
                content = """
                    {
                      "errors": [],
                      "messages": [],
                      "success": true,
                      "result": {
                        "content": "4.4.4.4",
                        "name": "osome.dev",
                        "proxied": true,
                        "type": "A",
                        "comment": "Domain verification record",
                        "comment_modified_on": "2024-01-01T05:20:00.12345Z",
                        "created_on": "2014-01-01T05:20:00.12345Z",
                        "id": "mock-dns-record-id",
                        "meta": {
                          "auto_added": true,
                          "source": "primary"
                        },
                        "modified_on": "2014-01-01T05:20:00.12345Z",
                        "proxiable": true,
                        "tags": [],
                        "tags_modified_on": "2025-01-01T05:20:00.12345Z",
                        "ttl": 3600
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
            else -> respondError(HttpStatusCode.InternalServerError)
        }
    }

    private val ipifyMockEngineSuccess = MockEngine { request ->
        when (request.url.fullPath) {
            "/?format=json" -> respond(
                content = """
                    {"ip":"2a00:1450:400f:80d::200e"}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )

            else -> respondBadRequest()
        }
    }

    private val client = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private val cloudflareService = CloudflareServiceImpl(
        client,
        "cloudflareApiKey"
    )

    private val cloudflareServiceFailedToken = CloudflareServiceImpl(
        client,
        "cloudflareApiKeyFailed"
    )

    private val ipifyServiceFailed = IpifyServiceImpl(client)

    private val ipifyServiceSuccess = IpifyServiceImpl(HttpClient(ipifyMockEngineSuccess) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    })

    @Test
    fun `GIVEN CloudflareDDNSController with incorrect Cloudflare token WHEN the request is failed AND system exit and SynologyOutput AUTH_FAILED`() = runBlocking {
        val controller = CloudflareDDNSController(
            cloudflareService = cloudflareServiceFailedToken,
            ipifyService = ipifyServiceFailed,
            hostnameList = "example.com",
            ipv4 = "1.2.3.4"
        )
        controller.setExitProcess(false)
        controller.verifyToken()
        assertEquals(SynologyOutput.AUTH_FAILED, controller.getLastOutput())
    }

    @Test
    fun `GIVEN CloudflareDDNSController with 4 domains without ipv6 WHEN the request is successful THEN return matched DNS record list with hostnameList`() = runBlocking {
        val controller = CloudflareDDNSController(
            cloudflareService = cloudflareService,
            ipifyService = ipifyServiceFailed,
            hostnameList = "osome.dev|api.osome.dev|*.osome.dev|example.com",
            ipv4 = "1.2.3.4"
        )
        controller.verifyToken()
        controller.matchHostnamesWithZones()
        assertEquals(3, controller.getDnsRecordListRequest().size)
    }

    @Test
    fun `GIVEN CloudflareDDNSController with 4 domains with ipv6 WHEN the request is successful THEN return matched DNS record list with hostnameList`() = runBlocking {
        val controller = CloudflareDDNSController(
            cloudflareService = cloudflareService,
            ipifyService = ipifyServiceSuccess,
            hostnameList = "osome.dev|api.osome.dev|*.osome.dev|example.com",
            ipv4 = "1.2.3.4"
        )
        controller.verifyToken()
        controller.matchHostnamesWithZones()
        assertEquals(6, controller.getDnsRecordListRequest().size)
    }

    @Test
    fun `GIVEN CloudflareDDNSController with 4 domains without ipv6 WHEN only 1 dns record is matched THEN return DNS record list with 1 record`() = runBlocking {
        val controller = CloudflareDDNSController(
            cloudflareService = cloudflareService,
            ipifyService = ipifyServiceFailed,
            hostnameList = "osome.dev|api.osome.dev|*.osome.dev|example.com",
            ipv4 = "1.2.3.4"
        )
        controller.verifyToken()
        controller.matchHostnamesWithZones()
        controller.setDnsRecords()
        assertEquals(1, controller.getDnsRecordList().size)
    }

    @Test
    fun `GIVEN CloudflareDDNSController with 3 domains without ipv6 WHEN 0 dns record is matched THEN exit with SynologyOutput DDNS_FAILED`() = runBlocking {
        val controller = CloudflareDDNSController(
            cloudflareService = cloudflareService,
            ipifyService = ipifyServiceFailed,
            hostnameList = "api.osome.dev|*.osome.dev|example.com",
            ipv4 = "1.2.3.4"
        )
        controller.setExitProcess(false)
        controller.verifyToken()
        controller.matchHostnamesWithZones()
        controller.setDnsRecords()
        assertEquals(SynologyOutput.DDNS_FAILED, controller.getLastOutput())
    }

    @Test
    fun `GIVEN CloudflareDDNSController with 4 domains with ipv6 WHEN only 1 dns record is matched THEN return DNS record list with 2 records`() = runBlocking {
        val controller = CloudflareDDNSController(
            cloudflareService = cloudflareService,
            ipifyService = ipifyServiceSuccess,
            hostnameList = "osome.dev|api.osome.dev|*.osome.dev|example.com",
            ipv4 = "1.2.3.4"
        )
        controller.verifyToken()
        controller.matchHostnamesWithZones()
        controller.setDnsRecords()
        val dnsRecordList = controller.getDnsRecordList()
        assertEquals(2, dnsRecordList.size)
        assertEquals(DnsRecordTypeEnumDto.A, dnsRecordList.first().type)
        assertEquals("1.2.3.4", dnsRecordList.first().content)
        assertEquals(DnsRecordTypeEnumDto.AAAA, dnsRecordList.last().type)
        assertEquals("2a00:1450:400f:80d::200e", dnsRecordList.last().content)
    }

    @Test
    fun `GIVEN CloudflareDDNSController with 4 domains with ipv6 WHEN only 1 dns record is matched THEN update 1 dns record and exit with SynologyOutput SUCCESS`() = runBlocking {
        val controller = CloudflareDDNSController(
            cloudflareService = cloudflareService,
            ipifyService = ipifyServiceSuccess,
            hostnameList = "osome.dev|api.osome.dev|*.osome.dev|example.com",
            ipv4 = "1.2.3.4"
        )
        controller.setExitProcess(false)
        controller.verifyToken()
        controller.matchHostnamesWithZones()
        controller.setDnsRecords()
        controller.updateDnsRecords()
        assertEquals(SynologyOutput.SUCCESS, controller.getLastOutput())
    }
}