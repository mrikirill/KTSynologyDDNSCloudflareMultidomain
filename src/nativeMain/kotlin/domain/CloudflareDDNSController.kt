package domain

import data.CloudflareService
import data.model.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException

class CloudflareDDNSController(
    private val cloudflareService: CloudflareService,
    private val ipv4: String,
    private val ipv6: String?,
    private val hostnameList: String
) {
    private val dnsRecordListRequest: MutableList<DnsRecordListRequestDto> = mutableListOf()
    private val dnsRecordList: MutableList<DnsRecordDto> = mutableListOf()
    private val dnsRecordUpdateList: MutableList<DnsRecordUpdateDto> = mutableListOf()

    suspend fun verifyToken() {
        try {
            val token = cloudflareService.verifyToken().result
            if (token.status != TokenStatus.ACTIVE) {
                throw SynologyException(
                    SynologyOutput.AUTH_FAILED,
                    "Token status is ${token.status}, expected ACTIVE"
                )
            }
        } catch (e: SynologyException) {
            throw e
        } catch (e: ClientRequestException) {
            throw SynologyException(
                SynologyOutput.AUTH_FAILED,
                "Token verification failed with HTTP ${e.response.status.value}: ${e.message}"
            )
        } catch (e: ServerResponseException) {
            throw SynologyException(
                SynologyOutput.DDNS_FAILED,
                "Cloudflare server error during token verification: ${e.response.status.value} - ${e.message}"
            )
        } catch (e: Exception) {
            throw SynologyException(
                SynologyOutput.BAD_CONN,
                "Connection failed during token verification: ${e::class.simpleName} - ${e.message}"
            )
        }
    }

    suspend fun matchHostnamesWithZones() {
        try {
            if (hostnameList.isEmpty()) {
                throw SynologyException(SynologyOutput.NO_HOSTNAME, "Hostname list is empty")
            }
            val hostnameList = extractHostnameList(hostnameList)
            val zones = cloudflareService.getZones()
            zones.result.forEach { zone ->
                hostnameList.forEach { hostname ->
                    if (!isHostnameFQDN(hostname)) {
                        throw SynologyException(
                            SynologyOutput.HOSTNAME_INCORRECT,
                            "Hostname '$hostname' is not a fully-qualified domain name"
                        )
                    }
                    if (hostname.contains(zone.name)) {
                         dnsRecordListRequest += DnsRecordListRequestDto(
                                zoneId = zone.id,
                                name = hostname,
                                type = DnsRecordTypeEnumDto.A
                         )

                        ipv6?.let {
                             dnsRecordListRequest += DnsRecordListRequestDto(
                                    zoneId = zone.id,
                                    name = hostname,
                                    type = DnsRecordTypeEnumDto.AAAA
                             )
                        }
                    }
                }
            }
            if (dnsRecordListRequest.isEmpty()) {
                throw SynologyException(
                    SynologyOutput.NO_HOSTNAME,
                    "No matching zones found for hostnames: ${hostnameList.joinToString(", ")}"
                )
            }
        } catch (e: SynologyException) {
            throw e
        } catch (e: ClientRequestException) {
            throw SynologyException(
                SynologyOutput.AUTH_FAILED,
                "Failed to fetch zones with HTTP ${e.response.status.value}: ${e.message}"
            )
        } catch (e: ServerResponseException) {
            throw SynologyException(
                SynologyOutput.DDNS_FAILED,
                "Cloudflare server error while fetching zones: ${e.response.status.value} - ${e.message}"
            )
        } catch (e: Exception) {
            throw SynologyException(
                SynologyOutput.BAD_CONN,
                "Connection error while fetching zones: ${e::class.simpleName} - ${e.message}"
            )
        }
    }

    suspend fun setDnsRecords() {
        val errors = mutableListOf<String>()
        dnsRecordListRequest.forEach { dnsRecordRequest ->
            try {
                val dnsRecords = cloudflareService.getDnsRecords(dnsRecordRequest)
                if (dnsRecords.result.size >= 1) {
                    // If multiple records exist, we update the first one.
                    // Ideally we should check if there are multiple and handle it, but for now we stick to existing logic
                    // but relaxed the check to >= 1 to avoid skipping if user has multiple records.
                    val dnsRecord = when (dnsRecordRequest.type) {
                        DnsRecordTypeEnumDto.A -> dnsRecords.result.first().copy(
                            content = ipv4,
                            zoneId = dnsRecordRequest.zoneId
                        )
                        DnsRecordTypeEnumDto.AAAA -> ipv6?.let {
                            dnsRecords.result.first().copy(
                                content = it,
                                zoneId = dnsRecordRequest.zoneId
                            )
                        }
                    }
                    dnsRecord?.let {
                         dnsRecordList += it
                    }
                }
            } catch (e: Exception) {
                errors += "Failed to get DNS record for ${dnsRecordRequest.name} (${dnsRecordRequest.type}): ${e::class.simpleName} - ${e.message}"
            }
        }
        if (dnsRecordList.isEmpty()) {
            throw SynologyException(
                SynologyOutput.DDNS_FAILED,
                "No DNS records could be retrieved. Errors: ${errors.joinToString("; ")}"
            )
        }
    }

    suspend fun updateDnsRecords() {
        val errors = mutableListOf<String>()
        dnsRecordList.forEach { dnsRecord ->
            try {
                val res = cloudflareService.updateDnsRecord(dnsRecord)
                if (res.success) {
                    dnsRecordUpdateList += res
                }
            } catch (e: Exception) {
                errors += "Failed to update DNS record ${dnsRecord.name} (${dnsRecord.type}): ${e::class.simpleName} - ${e.message}"
            }
        }
        if (dnsRecordUpdateList.isEmpty()) {
            throw SynologyException(
                SynologyOutput.BAD_HTTP_REQUEST,
                "No DNS records could be updated. Errors: ${errors.joinToString("; ")}"
            )
        } else {
            throw SynologyException(SynologyOutput.SUCCESS)
        }
    }

    private fun isHostnameFQDN(hostname: String): Boolean {
        return hostname.contains(".")
    }

    private fun extractHostnameList(hostnameList: String): List<String> {
        return hostnameList.split("|")
    }

    fun getDnsRecordListRequest(): List<DnsRecordListRequestDto> {
        return dnsRecordListRequest
    }

    fun getDnsRecordList(): List<DnsRecordDto> {
        return dnsRecordList
    }
}