package data.model

import kotlinx.serialization.Serializable

@Serializable
data class DnsRecordListDto(
    val success: Boolean,
    val result: List<DnsRecordDto>
)

