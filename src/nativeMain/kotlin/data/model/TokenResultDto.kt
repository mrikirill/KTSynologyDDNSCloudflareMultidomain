package data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResultDto(
    val id: String,
    val status: TokenStatus,
)

enum class TokenStatus {
    @SerialName("active")
    ACTIVE,
    @SerialName("expired")
    EXPIRED,
    @SerialName("disabled")
    DISABLED,
}