package domian

import data.CloudflareService
import data.IpifyService
import data.model.*
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

class CloudflareDDNSController(
    private val cloudflareService: CloudflareService,
    private val ipifyService: IpifyService,
    private val ipv4: String,
    private val hostnameList: String
) {
    private val dnsRecordListRequest: MutableList<DnsRecordListRequestDto> = mutableListOf()
    private val dnsRecordList: MutableList<DnsRecordDto> = mutableListOf()
    private val dnsRecordUpdateList: MutableList<DnsRecordUpdateDto> = mutableListOf()
    private var ipv6: String? = null
    private var lastOutput: String? = null
    private var exitProcess: Boolean = true

    init {
        runBlocking {
            ipv6 = try {
                ipifyService.getIpV6().ip
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun verifyToken() {
        try {
            val token = cloudflareService.verifyToken().result
            if (token.status != TokenStatus.ACTIVE) {
                exitWithSynologyOutput(SynologyOutput.AUTH_FAILED)
            }
        } catch (e: Exception) {
            println(e.message)
            exitWithSynologyOutput(SynologyOutput.AUTH_FAILED)
        }
    }

    suspend fun matchHostnamesWithZones() {
        try {
            if (hostnameList.isEmpty()) {
                exitWithSynologyOutput(SynologyOutput.NO_HOSTNAME)
            }
            val hostnameList = extractHostnameList(hostnameList)
            val zones = cloudflareService.getZones()
            zones.result.forEach { zone ->
                hostnameList.forEach { hostname ->
                    if (!isHostnameFQDN(hostname)) {
                        exitWithSynologyOutput(SynologyOutput.HOSTNAME_INCORRECT)
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
                exitWithSynologyOutput(SynologyOutput.NO_HOSTNAME)
            }
        } catch (e: Exception) {
            exitWithSynologyOutput(SynologyOutput.NO_HOSTNAME)
        }
    }

    suspend fun setDnsRecords() {
        dnsRecordListRequest.forEach { dnsRecordRequest ->
            try {
                val dnsRecords = cloudflareService.getDnsRecords(dnsRecordRequest)
                if (dnsRecords.result.size == 1) {
                    val dnsRecord = when (dnsRecordRequest.type) {
                        DnsRecordTypeEnumDto.A -> dnsRecords.result.first().copy(
                            content = ipv4
                        )
                        DnsRecordTypeEnumDto.AAAA -> ipv6?.let {
                            dnsRecords.result.first().copy(
                                content = it
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
            exitWithSynologyOutput(SynologyOutput.DDNS_FAILED)
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
            exitWithSynologyOutput(SynologyOutput.BAD_HTTP_REQUEST)
        } else {
            exitWithSynologyOutput(SynologyOutput.SUCCESS)
        }
    }

    private fun isHostnameFQDN(hostname: String): Boolean {
        return hostname.contains(".")
    }

    private fun extractHostnameList(hostnameList: String): List<String> {
        return hostnameList.split("|")
    }

    private fun exitWithSynologyOutput(output: String) {
        lastOutput = output
        if (!exitProcess) {
            return
        }
        println(output)
        exitProcess(0)
    }

    fun getDnsRecordListRequest(): List<DnsRecordListRequestDto> {
        return dnsRecordListRequest
    }

    fun getDnsRecordList(): List<DnsRecordDto> {
        return dnsRecordList
    }

    fun getLastOutput(): String? {
        return lastOutput
    }

    fun setExitProcess(exitProcess: Boolean) {
        this.exitProcess = exitProcess
    }
}