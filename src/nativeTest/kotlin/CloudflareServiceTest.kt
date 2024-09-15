
import data.CloudflareServiceImpl
import data.model.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class CloudflareServiceTest {

    private val mockEngine = MockEngine { request ->
        when (request.url.fullPath) {
            "/client/v4/user/tokens/verify" -> respond(
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

    private val service = CloudflareServiceImpl(
        client,
        "cloudflareApiKey"
    )

    @Test
    fun `GIVEN valid token WHEN verifyToken is called THEN returns valid token`() = runBlocking {
        val result = service.verifyToken()
        assertTrue(result.success)
    }

    @Test
    fun `GIVEN valid zones WHEN getZones is called THEN returns valid zones`() = runBlocking {
        val result = service.getZones()
        assertEquals(1, result.result.size)
    }

    @Test
    fun `GIVEN valid dns records WHEN getDnsRecords is called THEN returns valid dns records`() = runBlocking {
        val zones = service.getZones()
        assertEquals(1, zones.result.size)
        val result = service.getDnsRecords(
            DnsRecordListRequestDto(
                zoneId = zones.result.first().id,
                type = DnsRecordTypeEnumDto.A,
                name = "osome.dev"
            )
        )
        assertEquals(1, result.result.size)
        assertEquals(DnsRecordTypeEnumDto.A, result.result.first().type)
    }

    @Test
    fun `GIVEN valid dns record WHEN updateDnsRecord is called THEN returns updated dns record`() = runBlocking {
        val zones = service.getZones()
        assertEquals(1, zones.result.size)
        val dnsRecords = service.getDnsRecords(
            DnsRecordListRequestDto(
                zoneId = zones.result.first().id,
                type = DnsRecordTypeEnumDto.A,
                name = "osome.dev"
            )
        )
        assertEquals(1, dnsRecords.result.size)
        val result = service.updateDnsRecord(dnsRecords.result.first().copy(content = "4.4.4.4"))
        assertTrue(result.success)
    }
}