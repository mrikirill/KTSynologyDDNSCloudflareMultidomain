package data.model

import kotlinx.serialization.Serializable

@Serializable
data class ZoneListDto(
    val success: Boolean,
    val result: List<ZoneDto>
)
