package data.model

import kotlinx.serialization.Serializable

@Serializable
data class DnsRecordUpdateRequestDto(
    val id: String,
    val type: DnsRecordTypeEnumDto,
    val name: String,
    val content: String,
    val proxied: Boolean,
    val ttl: Int,
)