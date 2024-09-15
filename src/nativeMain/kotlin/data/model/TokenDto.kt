package data.model

import kotlinx.serialization.Serializable

@Serializable
data class TokenDto(
    val success: Boolean,
    val result: TokenResultDto
)
