package mihon.data.extension.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.domain.extension.model.ExtensionStore

/**
 * Extension store index in the format published by Aniyomi anime stores (e.g. Yūzōnō, Kohi-den).
 *
 * It matches [NetworkExtensionStore] (Mihon's manga format) except for the extension entry:
 * Aniyomi uses field 8 for the torrent flag and field 9 for the sources, while Mihon uses field 8
 * for the sources. Decoding an Aniyomi store that contains a torrent extension with the Mihon
 * model fails with "Error while decoding mihon.data.extension.model.NetworkExtensionStore".
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkAnimeExtensionStore(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val badgeLabel: String,
    @ProtoNumber(3) val signingKey: String,
    @ProtoNumber(4) val contact: NetworkExtensionStore.Contact,
    @ProtoNumber(101) val extensionList: ExtensionList? = null,
    @ProtoNumber(102) val extensionListUrl: String? = null,
) : BaseNetworkExtensionStore {
    @Serializable
    data class ExtensionList(
        @ProtoNumber(1) val extensions: List<Extension>,
    )

    @Serializable
    data class Extension(
        @ProtoNumber(1) val name: String,
        @ProtoNumber(2) val packageName: String,
        @ProtoNumber(3) val resources: NetworkExtensionStore.Resources,
        @ProtoNumber(4) val extensionLib: String,
        @ProtoNumber(5) val versionCode: Long,
        @ProtoNumber(6) val versionName: String,
        @ProtoNumber(7) val contentWarning: NetworkExtensionStore.ContentWarning,
        @ProtoNumber(8) val isTorrent: Boolean = false,
        @ProtoNumber(9) val sources: List<Source> = emptyList(),
    )

    @Serializable
    data class Source(
        @ProtoNumber(1) val id: Long,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(3) val language: String,
        @ProtoNumber(4) val homeUrl: String = "",
        @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
        @ProtoNumber(6) val message: String? = null,
    )

    override fun toExtensionStore(indexUrl: String): ExtensionStore {
        return ExtensionStore(
            indexUrl = indexUrl,
            name = name,
            badgeLabel = badgeLabel,
            signingKey = signingKey,
            contact = ExtensionStore.Contact(
                website = contact.website,
                discord = contact.discord,
            ),
            isLegacy = false,
            extensionListUrl = extensionListUrl,
        )
    }
}

/** Converts a store published in Mihon's format so anime stores using it keep working. */
fun NetworkExtensionStore.toAnimeExtensionStore(): NetworkAnimeExtensionStore {
    return NetworkAnimeExtensionStore(
        name = name,
        badgeLabel = badgeLabel,
        signingKey = signingKey,
        contact = contact,
        extensionList = extensionList?.toAnimeExtensionList(),
        extensionListUrl = extensionListUrl,
    )
}

fun NetworkExtensionStore.ExtensionList.toAnimeExtensionList(): NetworkAnimeExtensionStore.ExtensionList {
    return NetworkAnimeExtensionStore.ExtensionList(
        extensions = extensions.map { extension ->
            NetworkAnimeExtensionStore.Extension(
                name = extension.name,
                packageName = extension.packageName,
                resources = extension.resources,
                extensionLib = extension.extensionLib,
                versionCode = extension.versionCode,
                versionName = extension.versionName,
                contentWarning = extension.contentWarning,
                isTorrent = false,
                sources = extension.sources.map { source ->
                    NetworkAnimeExtensionStore.Source(
                        id = source.id,
                        name = source.name,
                        language = source.language,
                        homeUrl = source.homeUrl,
                        mirrorUrls = source.mirrorUrls,
                        message = source.message,
                    )
                },
            )
        },
    )
}
