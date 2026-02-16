package domain

class SynologyException(
    override val message: String,
    val detail: String? = null
) : Exception(message)
