package data.model

data class DnsRecordListRequestDto(
    val zoneId: String,
    val type: DnsRecordTypeEnumDto,
    val name: String,
)