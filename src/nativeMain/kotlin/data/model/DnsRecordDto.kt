package data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DnsRecordDto(
    val id: String,
    @SerialName("zone_id")
    val zoneId: String,
    val type: DnsRecordTypeEnumDto,
    val name: String,
    val content: String,
    val proxied: Boolean,
    val ttl: Int
) {
    fun toDnsRecordUpdateRequestDto(): DnsRecordUpdateRequestDto {
        return DnsRecordUpdateRequestDto(
            id = id,
            type = type,
            name = name,
            content = content,
            proxied = proxied,
            ttl = ttl,
        )
    }
}

@Serializable
enum class DnsRecordTypeEnumDto {
    @SerialName("A")
    A,
    @SerialName("AAAA")
    AAAA
}

@Serializable
data class IpifyResponseDto(
    val ip: String
)