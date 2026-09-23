package com.android.purebilibili.feature.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketException

/**
 * Manual SSDP discovery for DLNA MediaRenderer devices.
 *
 * Design notes (Android + OEM LAN discovery):
 * - Prefer Wi-Fi/Ethernet over cellular/VPN default routes.
 * - Hold [WifiManager.MulticastLock] for the whole discovery window.
 * - Join 239.255.255.250 so passive NOTIFY adverts are visible.
 * - Re-send M-SEARCH periodically; many TVs only answer the first few probes.
 */
object SsdpDiscovery {
    private const val TAG = "SsdpDiscovery"

    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val DEFAULT_TIMEOUT_MS = 8_000
    private const val RESEND_INTERVAL_MS = 1_500L
    private const val RECEIVE_SLICE_MS = 800

    private fun buildSearchPayload(searchTarget: String): String = """
        M-SEARCH * HTTP/1.1
        HOST: 239.255.255.250:1900
        MAN: "ssdp:discover"
        MX: 2
        ST: $searchTarget

    """.trimIndent().replace("\n", "\r\n")

    data class SsdpDevice(
        val location: String,
        val server: String,
        val usn: String,
        val st: String
    )

    /**
     * Run SSDP discovery.
     * @param timeoutMs total receive window in milliseconds
     */
    suspend fun discover(
        context: Context,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS,
    ): List<SsdpDevice> = withContext(Dispatchers.IO) {
        val devices = LinkedHashMap<String, SsdpDevice>()
        var socket: MulticastSocket? = null
        var multicastLock: WifiManager.MulticastLock? = null
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val previousProcessNetwork = runCatching { connectivityManager.boundNetworkForProcess }.getOrNull()
        var processNetworkBound = false

        try {
            Logger.i(TAG, "📺 [DLNA] Starting SSDP discovery (timeout: ${timeoutMs}ms)")
            multicastLock = acquireMulticastLock(context)

            val binding = resolveSsdpLocalNetworkBinding(connectivityManager)
            Logger.i(
                TAG,
                "📺 [DLNA] Preferred local network transport=${binding.transportLabel}, " +
                    "ip=${binding.ipv4Address?.hostAddress ?: "n/a"}, " +
                    "iface=${binding.networkInterface?.displayName ?: "n/a"}"
            )

            if (binding.transportLabel == "none") {
                Logger.w(TAG, "📺 [DLNA] No Wi-Fi/Ethernet network available; SSDP cannot reach LAN devices")
            }

            val localNetwork = binding.network
            if (localNetwork != null) {
                runCatching {
                    connectivityManager.bindProcessToNetwork(localNetwork)
                    processNetworkBound = true
                    Logger.i(TAG, "📺 [DLNA] Process bound to local network for discovery")
                }.onFailure { error ->
                    Logger.w(TAG, "📺 [DLNA] bindProcessToNetwork failed: ${error.message}")
                }
            }

            socket = openDiscoverySocket(binding)
            val multicastAddress = InetAddress.getByName(SSDP_ADDRESS)
            val joinedInterface = binding.networkInterface
            joinSsdpGroup(socket, multicastAddress, joinedInterface)

            Logger.d(TAG, "📺 [DLNA] Socket bound to local port ${socket.localPort}")

            val payloads = resolveSsdpSearchPayloads()
            val startTime = System.currentTimeMillis()
            var nextSendAt = 0L
            var sendRounds = 0
            var responseCount = 0

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val now = System.currentTimeMillis()
                if (now >= nextSendAt) {
                    sendMSearch(socket, multicastAddress, payloads)
                    sendRounds++
                    nextSendAt = now + RESEND_INTERVAL_MS
                    Logger.i(
                        TAG,
                        "📺 [DLNA] M-SEARCH round $sendRounds (${payloads.size} targets)"
                    )
                }

                val remaining = (timeoutMs - (System.currentTimeMillis() - startTime))
                    .toInt()
                    .coerceAtLeast(1)
                socket.soTimeout = remaining.coerceAtMost(RECEIVE_SLICE_MS)

                try {
                    val buffer = ByteArray(4096)
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)
                    responseCount++

                    val response = String(responsePacket.data, 0, responsePacket.length)
                    val device = parseResponse(response)
                    if (device != null) {
                        val key = device.usn.ifBlank { device.location }
                        if (key.isNotBlank() && key !in devices) {
                            devices[key] = device
                            Logger.i(
                                TAG,
                                "📺 [DLNA] Found device: server=${device.server.take(50)}, " +
                                    "type=${device.st.substringAfterLast(':')}"
                            )
                        }
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    // slice timeout; continue until total timeout
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            Logger.i(
                TAG,
                "📺 [DLNA] Discovery completed in ${elapsed}ms: " +
                    "rounds=$sendRounds, responses=$responseCount, unique=${devices.size}"
            )
            leaveSsdpGroup(socket, multicastAddress, joinedInterface)
        } catch (e: Exception) {
            Logger.e(TAG, "📺 [DLNA] Discovery error: ${e.javaClass.simpleName} - ${e.message}")
        } finally {
            runCatching { socket?.close() }
            if (processNetworkBound) {
                runCatching { connectivityManager.bindProcessToNetwork(previousProcessNetwork) }
            }
            releaseMulticastLock(multicastLock)
        }

        devices.values.toList()
    }

    private fun openDiscoverySocket(binding: SsdpLocalNetworkBinding): MulticastSocket {
        val socket = MulticastSocket(null)
        socket.reuseAddress = true
        socket.broadcast = true
        socket.timeToLive = 4

        // Bind the FD to the LAN Network *before* local bind when possible.
        binding.network?.let { network ->
            runCatching { network.bindSocket(socket) }
                .onSuccess { Logger.i(TAG, "📺 [DLNA] Socket FD bound to local Network object") }
                .onFailure { error ->
                    Logger.w(TAG, "📺 [DLNA] Network.bindSocket failed: ${error.message}")
                }
        }

        // SSDP NOTIFY packets are addressed to 239.255.255.250:1900. A socket bound to
        // the concrete LAN unicast address can send M-SEARCH but may not receive multicast
        // packets on Android/OEM kernels, even after successfully joining the group.
        // Bind INADDR_ANY:1900 so both multicast adverts and unicast M-SEARCH replies are
        // delivered; Network.bindSocket and networkInterface still pin traffic to Wi-Fi.
        socket.bind(InetSocketAddress(SSDP_PORT))
        Logger.i(TAG, "📺 [DLNA] Socket bound to wildcard local address on SSDP port $SSDP_PORT")

        binding.networkInterface?.let { nif ->
            runCatching {
                socket.networkInterface = nif
                Logger.i(TAG, "📺 [DLNA] Multicast out-interface=${nif.displayName}")
            }.onFailure { error ->
                Logger.w(TAG, "📺 [DLNA] setNetworkInterface failed: ${error.message}")
            }
        }

        return socket
    }

    private fun joinSsdpGroup(
        socket: MulticastSocket,
        group: InetAddress,
        networkInterface: NetworkInterface?,
    ) {
        try {
            if (networkInterface != null) {
                socket.joinGroup(InetSocketAddress(group, SSDP_PORT), networkInterface)
            } else {
                @Suppress("DEPRECATION")
                socket.joinGroup(group)
            }
            Logger.i(TAG, "📺 [DLNA] Joined SSDP multicast group")
        } catch (e: SocketException) {
            // Still usable for unicast M-SEARCH replies on many stacks.
            Logger.w(TAG, "📺 [DLNA] joinGroup failed (unicast replies may still work): ${e.message}")
        } catch (e: Exception) {
            Logger.w(TAG, "📺 [DLNA] joinGroup failed: ${e.message}")
        }
    }

    private fun leaveSsdpGroup(
        socket: MulticastSocket?,
        group: InetAddress,
        networkInterface: NetworkInterface?,
    ) {
        if (socket == null) return
        runCatching {
            if (networkInterface != null) {
                socket.leaveGroup(InetSocketAddress(group, SSDP_PORT), networkInterface)
            } else {
                @Suppress("DEPRECATION")
                socket.leaveGroup(group)
            }
        }
    }

    private fun sendMSearch(
        socket: MulticastSocket,
        multicastAddress: InetAddress,
        payloads: List<String>,
    ) {
        payloads.forEach { payload ->
            val data = payload.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(data, data.size, multicastAddress, SSDP_PORT)
            runCatching { socket.send(packet) }
                .onFailure { error ->
                    Logger.w(TAG, "📺 [DLNA] M-SEARCH send failed: ${error.message}")
                }
        }
    }

    private fun acquireMulticastLock(context: Context): WifiManager.MulticastLock? {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.createMulticastLock("SsdpDiscovery").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Logger.w(TAG, "📺 [DLNA] Failed to acquire multicast lock: ${e.message}")
            null
        }
    }

    private fun releaseMulticastLock(multicastLock: WifiManager.MulticastLock?) {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock.release()
            }
        } catch (e: Exception) {
            Logger.w(TAG, "📺 [DLNA] Failed to release multicast lock: ${e.message}")
        }
    }

    internal fun resolveSsdpSearchPayloads(): List<String> = listOf(
        "urn:schemas-upnp-org:device:MediaRenderer:1",
        "urn:schemas-upnp-org:service:AVTransport:1",
        "upnp:rootdevice",
        "ssdp:all"
    ).map(::buildSearchPayload).distinct()

    internal fun parseResponse(response: String): SsdpDevice? {
        val lines = response.split("\r\n", "\n")
        if (lines.isEmpty()) return null

        var location = ""
        var server = ""
        var usn = ""
        var st = ""
        var nt = ""
        var nts = ""

        for (line in lines.drop(1)) {
            val separator = line.indexOf(':')
            if (separator <= 0) continue
            val key = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            when {
                key.equals("LOCATION", ignoreCase = true) -> location = value
                key.equals("SERVER", ignoreCase = true) -> server = value
                key.equals("USN", ignoreCase = true) -> usn = value
                key.equals("ST", ignoreCase = true) -> st = value
                key.equals("NT", ignoreCase = true) -> nt = value
                key.equals("NTS", ignoreCase = true) -> nts = value
            }
        }

        if (!isUsableSsdpDiscoveryMessage(lines.first(), nts)) return null
        if (location.isEmpty()) return null
        if (usn.isEmpty()) {
            // Some broken renderers omit USN; still usable if LOCATION is present.
            usn = location
        }
        val type = st.ifBlank { nt }
        return SsdpDevice(location, server, usn, type)
    }
}
