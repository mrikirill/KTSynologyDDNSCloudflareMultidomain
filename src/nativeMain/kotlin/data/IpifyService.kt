package data

import data.model.IpifyResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

interface IpifyService {
    suspend fun getIpV6(): IpifyResponseDto
    suspend fun getIpV4(): IpifyResponseDto
}

class IpifyServiceImpl(
    private val httpClient: HttpClient,
): IpifyService {
    companion object {
        private const val BASE_URL_V6 = "https://api6.ipify.org"
        private const val BASE_URL_V4 = "https://api.ipify.org"
    }

    override suspend fun getIpV6(): IpifyResponseDto {
        return httpClient.get("$BASE_URL_V6/?format=json").body()
    }

    override suspend fun getIpV4(): IpifyResponseDto {
        return httpClient.get("$BASE_URL_V4/?format=json").body()
    }
}