package data.model

import kotlinx.serialization.Serializable

@Serializable
data class ZoneDto(
    val id: String,
    val name: String,
)