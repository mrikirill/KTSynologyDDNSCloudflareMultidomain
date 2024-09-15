package data

import data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

interface CloudflareService {
    suspend fun verifyToken(): TokenDto
    suspend fun getZones(): ZoneListDto
    suspend fun getDnsRecords(model: DnsRecordListRequestDto): DnsRecordListDto
    suspend fun updateDnsRecord(model: DnsRecordDto): DnsRecordUpdateDto
}

class CloudflareServiceImpl(
    private val httpClient: HttpClient,
    private val cloudflareApiKey: String
) : CloudflareService {
    companion object {
        private const val BASE_URL = "https://api.cloudflare.com/client/v4"
        private const val ZONES_PER_PAGE = 50
    }
    override suspend fun verifyToken(): TokenDto {
        return httpClient.get("$BASE_URL/user/tokens/verify"){
            headers {
                append(config.Config.HEADER_AUTHORIZATION, "Bearer $cloudflareApiKey")
            }
        }.body()
    }

    override suspend fun getZones(): ZoneListDto {
        return httpClient.get("$BASE_URL/zones?per_page=$ZONES_PER_PAGE&status=active"){
            headers {
                append(config.Config.HEADER_AUTHORIZATION, "Bearer $cloudflareApiKey")
            }
        }.body()
    }

    override suspend fun getDnsRecords(model: DnsRecordListRequestDto): DnsRecordListDto {
        return httpClient.get("$BASE_URL/zones/${model.zoneId}/dns_records?type=${model.type}&name=${model.name}"){
            headers {
                append(config.Config.HEADER_AUTHORIZATION, "Bearer $cloudflareApiKey")
            }
        }.body()
    }

    override suspend fun updateDnsRecord(model: DnsRecordDto): DnsRecordUpdateDto {
     return httpClient.patch("$BASE_URL/zones/${model.zoneId}/dns_records/${model.id}") {
         headers {
             append(config.Config.HEADER_AUTHORIZATION, "Bearer $cloudflareApiKey")
             append(HttpHeaders.ContentType, ContentType.Application.Json)
         }
         setBody(model.toDnsRecordUpdateRequestDto())
        }.body()
    }
}
