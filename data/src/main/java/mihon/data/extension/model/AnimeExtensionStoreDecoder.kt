package mihon.data.extension.model

import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Decodes protobuf anime store indexes.
 *
 * Tries the Aniyomi format first (what anime stores publish) and falls back to Mihon's format,
 * so stores built with either toolchain can be added.
 */
object AnimeExtensionStoreDecoder {

    fun decodeStore(protoBuf: ProtoBuf, bytes: ByteArray): NetworkAnimeExtensionStore {
        return decodeWithFallback(
            primary = { protoBuf.decodeFromByteArray<NetworkAnimeExtensionStore>(bytes) },
            fallback = { protoBuf.decodeFromByteArray<NetworkExtensionStore>(bytes).toAnimeExtensionStore() },
        )
    }

    fun decodeExtensionList(protoBuf: ProtoBuf, bytes: ByteArray): NetworkAnimeExtensionStore.ExtensionList {
        return decodeWithFallback(
            primary = { protoBuf.decodeFromByteArray<NetworkAnimeExtensionStore.ExtensionList>(bytes) },
            fallback = {
                protoBuf.decodeFromByteArray<NetworkExtensionStore.ExtensionList>(bytes).toAnimeExtensionList()
            },
        )
    }

    private inline fun <T> decodeWithFallback(primary: () -> T, fallback: () -> T): T {
        return try {
            primary()
        } catch (primaryError: Exception) {
            try {
                fallback()
            } catch (fallbackError: Exception) {
                primaryError.addSuppressed(fallbackError)
                throw primaryError
            }
        }
    }
}
