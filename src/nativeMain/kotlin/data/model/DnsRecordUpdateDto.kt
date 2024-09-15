package data.model

import kotlinx.serialization.Serializable

@Serializable
data class DnsRecordUpdateDto(
    val success: Boolean
)