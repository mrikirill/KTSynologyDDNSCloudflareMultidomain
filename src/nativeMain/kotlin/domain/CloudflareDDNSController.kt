package domain

import data.CloudflareService
import data.model.*

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
                throw SynologyException(SynologyOutput.AUTH_FAILED)
            }
        } catch (e: Exception) {
            if (e is SynologyException) throw e
            throw SynologyException(SynologyOutput.AUTH_FAILED)
        }
    }

    suspend fun matchHostnamesWithZones() {
        try {
            if (hostnameList.isEmpty()) {
                throw SynologyException(SynologyOutput.NO_HOSTNAME)
            }
            val hostnameList = extractHostnameList(hostnameList)
            val zones = cloudflareService.getZones()
            zones.result.forEach { zone ->
                hostnameList.forEach { hostname ->
                    if (!isHostnameFQDN(hostname)) {
                        throw SynologyException(SynologyOutput.HOSTNAME_INCORRECT)
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
                throw SynologyException(SynologyOutput.NO_HOSTNAME)
            }
        } catch (e: Exception) {
            if (e is SynologyException) throw e
            throw SynologyException(SynologyOutput.NO_HOSTNAME)
        }
    }

    suspend fun setDnsRecords() {
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
                // allow to continue
            }
        }
        if (dnsRecordList.isEmpty()) {
            throw SynologyException(SynologyOutput.DDNS_FAILED)
        }
    }

    suspend fun updateDnsRecords() {
        dnsRecordList.forEach { dnsRecord ->
            try {
                val res = cloudflareService.updateDnsRecord(dnsRecord)
                if (res.success) {
                    dnsRecordUpdateList += res
                }
            } catch (e: Exception) {
                // allow to continue
            }
        }
        if (dnsRecordUpdateList.isEmpty()) {
            throw SynologyException(SynologyOutput.BAD_HTTP_REQUEST)
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