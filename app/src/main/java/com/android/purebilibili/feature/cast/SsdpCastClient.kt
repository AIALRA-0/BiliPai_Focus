package com.android.purebilibili.feature.cast

import com.android.purebilibili.core.plugin.CastPluginPlaybackState
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

/**
 * SSDP/DLNA caster.
 * 通过 SOAP 调 AVTransport。
 */
object SsdpCastClient {
    private const val TAG = "SsdpCastClient"
    private const val POLL_INTERVAL_MS = 1_000L
    private val soapContentType = "text/xml; charset=utf-8".toMediaType()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _playbackState = MutableStateFlow(CastPluginPlaybackState())
    val playbackState: StateFlow<CastPluginPlaybackState> = _playbackState.asStateFlow()

    private var activeEndpoint: AvTransportEndpoint? = null
    private var pollJob: Job? = null

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        // Local device description endpoints are often slow or half-awake.
        .connectTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    data class AvTransportEndpoint(
        val controlUrl: String,
        val serviceType: String
    )

    data class SsdpDeviceProfile(
        val friendlyName: String,
        val modelName: String?,
        val avTransportEndpoint: AvTransportEndpoint?
    )

    suspend fun cast(
        device: SsdpDiscovery.SsdpDevice,
        mediaUrl: String,
        title: String,
        creator: String,
        startPositionMs: Long = 0L,
        autoplay: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val profile = fetchDeviceProfile(device.location)
            val endpoint = profile?.avTransportEndpoint
                ?: error("设备不支持 AVTransport 控制")
            val metadata = buildDidlMetadata(mediaUrl, title, creator)

            sendSoapAction(
                endpoint = endpoint,
                action = "SetAVTransportURI",
                actionBody = buildSetUriActionBody(endpoint.serviceType, mediaUrl, metadata)
            )

            if (startPositionMs > 0L) {
                runCatching {
                    sendSoapAction(
                        endpoint = endpoint,
                        action = "Seek",
                        actionBody = buildSeekActionBody(endpoint.serviceType, startPositionMs)
                    )
                }.onFailure { error ->
                    Logger.w(TAG, "📺 [SSDP] Initial seek failed: ${error.message}")
                }
            }

            if (autoplay) {
                sendSoapAction(
                    endpoint = endpoint,
                    action = "Play",
                    actionBody = buildPlayActionBody(endpoint.serviceType)
                )
            }

            attachSession(
                endpoint = endpoint,
                deviceLabel = profile.friendlyName.ifBlank { device.server },
                title = title,
                isPlaying = autoplay
            )
            Logger.i(TAG, "📺 [SSDP] Cast command sent to ${device.server.take(40)}")
        }
    }

    suspend fun play(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = requireActiveEndpoint()
            sendSoapAction(
                endpoint = endpoint,
                action = "Play",
                actionBody = buildPlayActionBody(endpoint.serviceType)
            )
            _playbackState.update { it.copy(isPlaying = true, isBuffering = false) }
        }
    }

    suspend fun pause(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = requireActiveEndpoint()
            sendSoapAction(
                endpoint = endpoint,
                action = "Pause",
                actionBody = buildPauseActionBody(endpoint.serviceType)
            )
            _playbackState.update { it.copy(isPlaying = false, isBuffering = false) }
        }
    }

    suspend fun seek(positionMs: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val safePositionMs = positionMs.coerceAtLeast(0L)
            val endpoint = requireActiveEndpoint()
            sendSoapAction(
                endpoint = endpoint,
                action = "Seek",
                actionBody = buildSeekActionBody(endpoint.serviceType, safePositionMs)
            )
            _playbackState.update {
                it.copy(
                    currentPositionMs = safePositionMs,
                    bufferedPositionMs = maxOf(it.bufferedPositionMs, safePositionMs)
                )
            }
        }
    }

    fun clearPlaybackSession() {
        pollJob?.cancel()
        pollJob = null
        activeEndpoint = null
        _playbackState.value = CastPluginPlaybackState()
    }

    suspend fun fetchDeviceProfile(
        device: SsdpDiscovery.SsdpDevice
    ): SsdpDeviceProfile? = withContext(Dispatchers.IO) {
        fetchDeviceProfile(device.location)
    }

    private fun fetchDeviceProfile(descriptionLocation: String): SsdpDeviceProfile? {
        return runCatching {
            val request = Request.Builder()
                .url(descriptionLocation)
                // Some TV firmwares reject empty / generic clients.
                .header("User-Agent", "BiliPai/1.0 UPnP/1.0 DLNADOC/1.50")
                .header("Accept", "text/xml, application/xml, */*")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Logger.w(TAG, "📺 [SSDP] Fetch device description failed: ${response.code}")
                    return null
                }
                val descriptionXml = response.body.string()
                val profile = parseDeviceProfile(descriptionXml, descriptionLocation)
                if (profile == null) {
                    Logger.w(TAG, "📺 [SSDP] Device description parse returned null")
                } else if (profile.avTransportEndpoint == null) {
                    Logger.w(
                        TAG,
                        "📺 [SSDP] Device has no AVTransport: name=${profile.friendlyName.take(40)}"
                    )
                }
                return profile
            }
        }.getOrElse { error ->
            Logger.w(TAG, "📺 [SSDP] Fetch device profile exception: ${error.message}")
            null
        }
    }

    internal fun parseAvTransportEndpoint(
        descriptionXml: String,
        descriptionLocation: String
    ): AvTransportEndpoint? = parseDeviceProfile(
        descriptionXml = descriptionXml,
        descriptionLocation = descriptionLocation
    )?.avTransportEndpoint

    internal fun parseDeviceProfile(
        descriptionXml: String,
        descriptionLocation: String
    ): SsdpDeviceProfile? {
        if (descriptionXml.isBlank()) return null
        return runCatching {
            val document = parseDocument(descriptionXml)
            val deviceNodes = document.getElementsByTagNameNS("*", "device")
            if (deviceNodes.length == 0) return null

            // Root devices often embed MediaRenderer; prefer the node that actually owns AVTransport.
            var selectedDevice: Element? = null
            var endpoint: AvTransportEndpoint? = null
            for (i in 0 until deviceNodes.length) {
                val device = deviceNodes.item(i) as? Element ?: continue
                val candidate = findDirectAvTransportEndpoint(device, descriptionLocation) ?: continue
                val deviceType = device.getFirstChildContent("deviceType")
                val isRenderer = deviceType.contains("MediaRenderer", ignoreCase = true)
                if (endpoint == null || isRenderer) {
                    selectedDevice = device
                    endpoint = candidate
                    if (isRenderer) break
                }
            }

            val fallbackDevice = deviceNodes.item(0) as? Element
            val nameSource = selectedDevice ?: fallbackDevice ?: return null
            SsdpDeviceProfile(
                friendlyName = nameSource.getFirstChildContent("friendlyName"),
                modelName = nameSource.getFirstChildContent("modelName").ifBlank { null },
                avTransportEndpoint = endpoint
            )
        }.getOrElse { error ->
            Logger.w(TAG, "📺 [SSDP] Parse description failed: ${error.message}")
            null
        }
    }

    /**
     * Inspect this device node's own serviceList only (ignore nested embedded devices).
     */
    private fun findDirectAvTransportEndpoint(
        device: Element,
        descriptionLocation: String,
    ): AvTransportEndpoint? {
        val serviceList = device.directChildElement("serviceList") ?: return null
        var child = serviceList.firstChild
        while (child != null) {
            val service = child as? Element
            child = child.nextSibling
            if (service == null || !service.localOrTagName().equals("service", ignoreCase = true)) {
                continue
            }
            val serviceType = service.getFirstChildContent("serviceType")
            if (!serviceType.contains("AVTransport", ignoreCase = true)) continue
            val controlUrlRaw = service.getFirstChildContent("controlURL")
            if (controlUrlRaw.isBlank()) continue
            return AvTransportEndpoint(
                controlUrl = URI(descriptionLocation).resolve(controlUrlRaw.trim()).toString(),
                serviceType = serviceType
            )
        }
        return null
    }

    private fun Element.directChildElement(tagName: String): Element? {
        var child = firstChild
        while (child != null) {
            val element = child as? Element
            if (element != null && element.localOrTagName().equals(tagName, ignoreCase = true)) {
                return element
            }
            child = child.nextSibling
        }
        return null
    }

    private fun Element.localOrTagName(): String {
        val local = localName
        if (!local.isNullOrBlank()) return local
        return tagName.substringAfter(':')
    }

    internal fun buildSetUriActionBody(
        serviceType: String,
        mediaUrl: String,
        metadata: String
    ): String {
        val escapedMediaUrl = escapeXml(mediaUrl)
        val escapedMetadata = escapeXml(metadata)
        return """
            <u:SetAVTransportURI xmlns:u="$serviceType">
                <InstanceID>0</InstanceID>
                <CurrentURI>$escapedMediaUrl</CurrentURI>
                <CurrentURIMetaData>$escapedMetadata</CurrentURIMetaData>
            </u:SetAVTransportURI>
        """.trimIndent()
    }

    internal fun buildSeekActionBody(serviceType: String, positionMs: Long): String = """
        <u:Seek xmlns:u="$serviceType">
            <InstanceID>0</InstanceID>
            <Unit>REL_TIME</Unit>
            <Target>${formatDlnaTime(positionMs)}</Target>
        </u:Seek>
    """.trimIndent()

    internal fun parseDlnaTimeMs(value: String): Long {
        val parts = value.trim().split(":")
        if (parts.size != 3) return 0L
        val hours = parts[0].toLongOrNull() ?: return 0L
        val minutes = parts[1].toLongOrNull() ?: return 0L
        val seconds = parts[2].toDoubleOrNull() ?: return 0L
        if (hours < 0L || minutes !in 0L..59L || seconds < 0.0 || seconds >= 60.0) return 0L
        return ((hours * 60L + minutes) * 60_000L + (seconds * 1_000.0).toLong())
    }

    private fun buildPlayActionBody(serviceType: String): String = """
        <u:Play xmlns:u="$serviceType">
            <InstanceID>0</InstanceID>
            <Speed>1</Speed>
        </u:Play>
    """.trimIndent()

    private fun buildPauseActionBody(serviceType: String): String = """
        <u:Pause xmlns:u="$serviceType">
            <InstanceID>0</InstanceID>
        </u:Pause>
    """.trimIndent()

    private fun attachSession(
        endpoint: AvTransportEndpoint,
        deviceLabel: String,
        title: String,
        isPlaying: Boolean
    ) {
        pollJob?.cancel()
        activeEndpoint = endpoint
        _playbackState.value = CastPluginPlaybackState(
            isActive = true,
            deviceLabel = deviceLabel,
            title = title,
            isPlaying = isPlaying,
            canSeek = true
        )
        pollJob = scope.launch {
            while (isActive && activeEndpoint == endpoint) {
                refreshPlaybackState(endpoint)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun refreshPlaybackState(endpoint: AvTransportEndpoint) {
        val transportState = runCatching {
            parseDocument(
                sendSoapAction(
                    endpoint = endpoint,
                    action = "GetTransportInfo",
                    actionBody = buildGetTransportInfoActionBody(endpoint.serviceType)
                )
            ).firstTagText("CurrentTransportState")
        }.getOrNull()
        val positionInfo = runCatching {
            val document = parseDocument(
                sendSoapAction(
                    endpoint = endpoint,
                    action = "GetPositionInfo",
                    actionBody = buildGetPositionInfoActionBody(endpoint.serviceType)
                )
            )
            PlaybackPosition(
                positionMs = parseDlnaTimeMs(document.firstTagText("RelTime")),
                durationMs = parseDlnaTimeMs(document.firstTagText("TrackDuration"))
            )
        }.getOrNull()

        if (transportState == null && positionInfo == null) return
        val normalizedState = transportState.orEmpty().uppercase()
        _playbackState.update { current ->
            current.copy(
                isActive = true,
                isPlaying = normalizedState == "PLAYING" ||
                    (transportState == null && current.isPlaying),
                isBuffering = normalizedState == "TRANSITIONING",
                currentPositionMs = positionInfo?.positionMs ?: current.currentPositionMs,
                durationMs = positionInfo?.durationMs?.takeIf { it > 0L } ?: current.durationMs,
                bufferedPositionMs = positionInfo?.positionMs ?: current.bufferedPositionMs,
                canSeek = (positionInfo?.durationMs ?: current.durationMs) > 0L
            )
        }
    }

    private data class PlaybackPosition(
        val positionMs: Long,
        val durationMs: Long
    )

    private fun requireActiveEndpoint(): AvTransportEndpoint =
        activeEndpoint ?: error("无活动投屏会话")

    private fun buildGetTransportInfoActionBody(serviceType: String): String = """
        <u:GetTransportInfo xmlns:u="$serviceType">
            <InstanceID>0</InstanceID>
        </u:GetTransportInfo>
    """.trimIndent()

    private fun buildGetPositionInfoActionBody(serviceType: String): String = """
        <u:GetPositionInfo xmlns:u="$serviceType">
            <InstanceID>0</InstanceID>
        </u:GetPositionInfo>
    """.trimIndent()

    private fun formatDlnaTime(positionMs: Long): String {
        val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
        return "%02d:%02d:%02d".format(
            totalSeconds / 3_600L,
            (totalSeconds / 60L) % 60L,
            totalSeconds % 60L
        )
    }

    private fun buildDidlMetadata(url: String, title: String, creator: String): String {
        val escapedUrl = escapeXml(url)
        val escapedTitle = escapeXml(title.ifBlank { "BiliPai Video" })
        val escapedCreator = escapeXml(creator.ifBlank { "BiliPai" })
        return """
            <DIDL-Lite xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"
                xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/">
                <item id="1" parentID="0" restricted="1">
                    <dc:title>$escapedTitle</dc:title>
                    <upnp:class>object.item.videoItem</upnp:class>
                    <dc:creator>$escapedCreator</dc:creator>
                    <res protocolInfo="http-get:*:video/mp4:*">$escapedUrl</res>
                </item>
            </DIDL-Lite>
        """.trimIndent()
    }

    private fun parseDocument(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            // Android's XML implementations vary by release. Do not reject a
            // legitimate UPnP description because one optional feature is missing.
            runCatching {
                setFeature("http://xml.org/sax/features/external-general-entities", false)
            }
            runCatching {
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            runCatching {
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }
            runCatching { setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true) }
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
        }
        return factory.newDocumentBuilder().apply {
            // Keep external resources disabled even when a feature flag is absent.
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }.parse(InputSource(StringReader(xml)))
    }

    private fun Document.firstTagText(tagName: String): String {
        val namespaced = getElementsByTagNameNS("*", tagName)
        if (namespaced.length > 0) return namespaced.item(0)?.textContent?.trim().orEmpty()
        val plain = getElementsByTagName(tagName)
        return if (plain.length > 0) plain.item(0)?.textContent?.trim().orEmpty() else ""
    }

    private fun sendSoapAction(
        endpoint: AvTransportEndpoint,
        action: String,
        actionBody: String
    ): String {
        val envelope = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    $actionBody
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val request = Request.Builder()
            .url(endpoint.controlUrl)
            .header("SOAPACTION", "\"${endpoint.serviceType}#$action\"")
            .post(envelope.toRequestBody(soapContentType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val payload = response.body.string().take(180)
                error("SOAP $action failed (${response.code}): $payload")
            }
            return response.body.string()
        }
    }

    private fun escapeXml(value: String): String = buildString(value.length + 16) {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }

    private fun Element.getFirstChildContent(tagName: String): String {
        val nodes = getElementsByTagNameNS("*", tagName)
        if (nodes.length == 0) return ""
        return nodes.item(0)?.textContent?.trim().orEmpty()
    }
}
