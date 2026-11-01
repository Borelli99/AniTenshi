package mihon.data.extension.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.zip.GZIPInputStream

class AnimeExtensionStoreDecoderTest {

    private val protoBuf = ProtoBuf

    @Test
    fun `aniyomi store with torrent extensions fails with the mihon model`() {
        val bytes = yuzonoIndex()

        assertThrows<Exception> {
            protoBuf.decodeFromByteArray<NetworkExtensionStore>(bytes)
        }
    }

    @Test
    fun `aniyomi store with torrent extensions is decoded`() {
        val store = AnimeExtensionStoreDecoder.decodeStore(protoBuf, yuzonoIndex())
        val extensions = store.extensionList!!.extensions

        store.name shouldBe "Yūzōnō"
        extensions.size shouldBe 247
        extensions.all { it.sources.isNotEmpty() } shouldBe true

        val animeSaturn = extensions.single { it.packageName == "eu.kanade.tachiyomi.animeextension.it.animesaturn" }
        animeSaturn.isTorrent shouldBe false
        animeSaturn.sources.single().language shouldBe "it"

        val nyaa = extensions.single { it.packageName == "eu.kanade.tachiyomi.animeextension.all.nyaatorrent" }
        nyaa.isTorrent shouldBe true
    }

    @Test
    fun `mihon store is still decoded`() {
        val bytes = protoBuf.encodeToByteArray(mihonStore())

        val store = AnimeExtensionStoreDecoder.decodeStore(protoBuf, bytes)
        val extension = store.extensionList!!.extensions.single()

        store.name shouldBe "Mihon format"
        extension.isTorrent shouldBe false
        extension.sources.single().language shouldBe "it"
        extension.sources.single().homeUrl shouldBe "https://example.org"
    }

    @Test
    fun `mihon extension list is still decoded`() {
        val bytes = protoBuf.encodeToByteArray(mihonStore().extensionList!!)

        val list = AnimeExtensionStoreDecoder.decodeExtensionList(protoBuf, bytes)

        list.extensions.single().sources.single().name shouldBe "Example"
    }

    private fun yuzonoIndex(): ByteArray {
        val stream = javaClass.getResourceAsStream("/extension-store/yuzono-anime-index.pb.gz")!!
        return GZIPInputStream(stream).use { it.readBytes() }
    }

    private fun mihonStore() = NetworkExtensionStore(
        name = "Mihon format",
        badgeLabel = "MF",
        signingKey = "key",
        contact = NetworkExtensionStore.Contact(website = "https://example.org", discord = null),
        extensionList = NetworkExtensionStore.ExtensionList(
            extensions = listOf(
                NetworkExtensionStore.Extension(
                    name = "Example",
                    packageName = "eu.kanade.tachiyomi.animeextension.it.example",
                    resources = NetworkExtensionStore.Resources(
                        apkUrl = "https://example.org/example.apk",
                        iconUrl = "https://example.org/example.png",
                    ),
                    extensionLib = "16",
                    versionCode = 1,
                    versionName = "16.1",
                    contentWarning = NetworkExtensionStore.ContentWarning.SAFE,
                    sources = listOf(
                        NetworkExtensionStore.Source(
                            id = 1,
                            name = "Example",
                            language = "it",
                            homeUrl = "https://example.org",
                        ),
                    ),
                ),
            ),
        ),
        extensionListUrl = null,
    )
}
