package domain

data class SynologyInput (
    val cloudflareApiKey: String,
    val hostnameList: String,
    val ip: String
)