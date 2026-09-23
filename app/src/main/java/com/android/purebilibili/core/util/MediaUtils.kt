package com.android.purebilibili.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecList
import android.os.Build
import android.view.Display
import androidx.media3.common.MimeTypes
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import java.util.concurrent.ConcurrentHashMap

object MediaUtils {
    // 解码器探测结果缓存：codecInfos 在进程生命周期内不变，避免每次切画质都重新枚举
    private val decoderSupportCache = ConcurrentHashMap<String, Boolean>()
    /**
     * Check if HEVC (H.265) decoder is supported
     */
    fun isHevcSupported(): Boolean {
        return hasDecoder("video/hevc")
    }

    /**
     * Check if AV1 decoder is supported
     */
    fun isAv1Supported(): Boolean {
        // AV1 support is limited on older devices
        return hasDecoder("video/av01")
    }

    /** 检查平台或应用内置 FFmpeg 是否能够解码 E-AC-3/JOC 音轨。 */
    fun isDolbyAtmosAudioSupported(): Boolean {
        return isPlatformDolbyAudioDecoderSupported() || isDolbySoftwareAudioDecoderSupported()
    }

    /** 平台解码器可用时保留 Dolby JOC/Atmos 渲染链路。 */
    fun isPlatformDolbyAudioDecoderSupported(): Boolean {
        return hasDecoder(MimeTypes.AUDIO_E_AC3) || hasDecoder(MimeTypes.AUDIO_E_AC3_JOC)
    }

    /** 检查应用内置的窄版 FFmpeg 是否包含 E-AC-3 解码器。 */
    // FfmpegLibrary 属 media3 unstable API：应用在稳定能力查询封装后消费，opt-in 标记会级联污染全部调用方。
    @SuppressLint("UnsafeOptInUsageError")
    fun isDolbySoftwareAudioDecoderSupported(): Boolean {
        return runCatching {
            FfmpegLibrary.supportsFormat(MimeTypes.AUDIO_E_AC3)
        }.onFailure { error ->
            Logger.e("MediaUtils", "Failed to load bundled E-AC-3 decoder", error)
        }.getOrDefault(false)
    }

    /** 只有平台不支持而 FFmpeg 可用时，实际播放才属于兼容软解。 */
    fun isDolbySoftwareAudioDecoderRequired(): Boolean {
        return !isPlatformDolbyAudioDecoderSupported() && isDolbySoftwareAudioDecoderSupported()
    }

    /**
     * Check if HDR (HDR10/HLG) video is supported
     * HDR requires both decoder support and display capability
     */
    fun isHdrSupported(context: Context? = null): Boolean {
        // HDR10 uses HEVC with specific profile
        // Check for HEVC support first, then verify display HDR capability when context is available
        if (!hasDecoder("video/hevc") || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }
        if (context == null) {
            return true
        }
        return hasHdrDisplaySupport(context)
    }
    
    /**
     * Check if Dolby Vision is supported
     * Dolby Vision requires specific hardware decoder
     */
    fun isDolbyVisionSupported(context: Context? = null): Boolean {
        // Dolby Vision MIME type
        val hasDolbyDecoder = hasDecoder("video/dolby-vision") || hasDecoder("video/dvhe") || hasDecoder("video/dvav")
        if (!hasDolbyDecoder || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }
        if (context == null) {
            return true
        }
        return hasDolbyVisionDisplaySupport(context)
    }

    private fun hasHdrDisplaySupport(context: Context): Boolean {
        return supportsGenericHdrTypes(getSupportedHdrTypes(context))
    }

    private fun hasDolbyVisionDisplaySupport(context: Context): Boolean {
        return supportsDolbyVisionType(getSupportedHdrTypes(context))
    }

    private fun getSupportedHdrTypes(context: Context): IntArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return null
        }

        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
            display?.hdrCapabilities?.supportedHdrTypes
        } catch (e: Exception) {
            Logger.e("MediaUtils", "Failed to read display HDR capabilities", e)
            null
        }
    }

    internal fun supportsGenericHdrTypes(supportedHdrTypes: IntArray?): Boolean {
        if (supportedHdrTypes == null || supportedHdrTypes.isEmpty()) {
            return false
        }
        for (type in supportedHdrTypes) {
            if (type == Display.HdrCapabilities.HDR_TYPE_HDR10 ||
                type == Display.HdrCapabilities.HDR_TYPE_HLG
            ) {
                return true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                type == Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS
            ) {
                return true
            }
        }
        return false
    }

    internal fun supportsDolbyVisionType(supportedHdrTypes: IntArray?): Boolean {
        val types = supportedHdrTypes ?: return false
        if (types.isEmpty()) {
            return false
        }
        return types.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)
    }

    private fun hasDecoder(mimeType: String): Boolean {
        decoderSupportCache[mimeType]?.let { return it }
        val supported = queryDecoderSupport(mimeType)
        decoderSupportCache[mimeType] = supported
        return supported
    }

    private fun queryDecoderSupport(mimeType: String): Boolean {
        try {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val codecs = list.codecInfos
            for (codec in codecs) {
                if (codec.isEncoder) continue
                val types = codec.supportedTypes
                for (type in types) {
                    if (type.equals(mimeType, ignoreCase = true)) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("MediaUtils", "Failed to check decoder support for $mimeType", e)
        }
        return false
    }
}
