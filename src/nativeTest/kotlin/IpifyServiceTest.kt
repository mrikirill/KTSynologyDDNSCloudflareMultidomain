import data.IpifyServiceImpl
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class IpifyServiceTest {
    private val mockEngine = MockEngine { request ->
        when (request.url.fullPath) {
            "/?format=json" -> respond(
                content = """
                    {"ip":"2a00:1450:400f:80d::200e"}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )

            else -> respondBadRequest()
        }
    }

    private val client = HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private val ipifyService = IpifyServiceImpl(client)

    @Test
    fun `GIVEN a request to get IPv6 from Ipify service WHEN the request is successful THEN return the IPv6`() = runBlocking {
        val response = ipifyService.getIpV6()
        assertEquals("2a00:1450:400f:80d::200e", response.ip)
    }
}