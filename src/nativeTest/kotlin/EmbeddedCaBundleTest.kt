import config.EMBEDDED_CA_BUNDLE
import kotlin.test.Test
import kotlin.test.assertTrue

class EmbeddedCaBundleTest {

    @Test
    fun `embedded CA bundle is not empty`() {
        assertTrue(EMBEDDED_CA_BUNDLE.isNotBlank(), "Embedded CA bundle should not be empty")
    }

    @Test
    fun `embedded CA bundle contains valid PEM certificates`() {
        assertTrue(
            EMBEDDED_CA_BUNDLE.contains("-----BEGIN CERTIFICATE-----"),
            "CA bundle should contain at least one BEGIN CERTIFICATE marker"
        )
        assertTrue(
            EMBEDDED_CA_BUNDLE.contains("-----END CERTIFICATE-----"),
            "CA bundle should contain at least one END CERTIFICATE marker"
        )
    }

    @Test
    fun `embedded CA bundle has matching BEGIN and END markers`() {
        val beginCount = "-----BEGIN CERTIFICATE-----".toRegex().findAll(EMBEDDED_CA_BUNDLE).count()
        val endCount = "-----END CERTIFICATE-----".toRegex().findAll(EMBEDDED_CA_BUNDLE).count()
        assertTrue(beginCount > 0, "CA bundle should contain certificates")
        assertTrue(
            beginCount == endCount,
            "BEGIN ($beginCount) and END ($endCount) certificate markers should match"
        )
    }

    @Test
    fun `embedded CA bundle contains reasonable number of root certificates`() {
        val certCount = "-----BEGIN CERTIFICATE-----".toRegex().findAll(EMBEDDED_CA_BUNDLE).count()
        assertTrue(
            certCount >= 50,
            "CA bundle should contain at least 50 root certificates, found $certCount"
        )
    }
}
