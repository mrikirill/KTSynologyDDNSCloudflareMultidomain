package data

import data.model.IpifyResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

interface IpifyService {
    suspend fun getIpV6(): IpifyResponseDto
}

class IpifyServiceImpl(
    private val httpClient: HttpClient,
): IpifyService {
    companion object {
        private const val BASE_URL = "https://api6.ipify.org"
    }

    override suspend fun getIpV6(): IpifyResponseDto {
        return httpClient.get("$BASE_URL/?format=json").body()
    }
}