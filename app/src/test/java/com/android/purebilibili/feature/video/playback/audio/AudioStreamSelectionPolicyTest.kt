package com.android.purebilibili.feature.video.playback.audio

import com.android.purebilibili.data.model.response.Dash
import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.Dolby
import com.android.purebilibili.data.model.response.Flac
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioStreamSelectionPolicyTest {

    private val standard192 = DashAudio(
        id = 30280,
        baseUrl = "https://example.com/audio-192.m4s",
        bandwidth = 192_000
    )
    private val standard64 = DashAudio(
        id = 30216,
        baseUrl = "https://example.com/audio-64.m4s",
        bandwidth = 64_000
    )
    private val dolby = DashAudio(
        id = AUDIO_QUALITY_DOLBY,
        baseUrl = "https://example.com/audio-dolby.m4s",
        bandwidth = 448_000,
        mimeType = "audio/mp4",
        codecs = "ec-3"
    )
    private val hiRes = DashAudio(
        id = AUDIO_QUALITY_HI_RES,
        baseUrl = "https://example.com/audio-hires.m4s",
        bandwidth = 1_800_000,
        mimeType = "audio/mp4",
        codecs = "fLaC"
    )

    @Test
    fun `collect candidates includes standard dolby and hi res streams`() {
        val candidates = collectAudioStreamCandidates(fullDash())

        assertEquals(
            listOf(AudioStreamKind.HI_RES, AudioStreamKind.DOLBY, AudioStreamKind.STANDARD, AudioStreamKind.STANDARD),
            candidates.map { it.kind }
        )
        assertEquals(
            listOf(AUDIO_QUALITY_HI_RES, AUDIO_QUALITY_DOLBY, 30280, 30216),
            candidates.map { it.preferenceId }
        )
    }

    @Test
    fun `auto selects best standard audio without enabling premium audio`() {
        val decision = resolveAudioStreamSelection(
            dash = fullDash(),
            requestedAudioQuality = AUDIO_QUALITY_AUTO
        )

        assertEquals(AUDIO_QUALITY_AUTO, decision.selectedPreferenceId)
        assertEquals(AudioStreamKind.STANDARD, decision.selected?.kind)
        assertEquals(30280, decision.selected?.track?.id)
        assertNull(decision.fallbackReason)
    }

    @Test
    fun `explicit hi res selection returns flac candidate`() {
        val decision = resolveAudioStreamSelection(
            dash = fullDash(),
            requestedAudioQuality = AUDIO_QUALITY_HI_RES
        )

        assertEquals(AUDIO_QUALITY_HI_RES, decision.selectedPreferenceId)
        assertEquals("https://example.com/audio-hires.m4s", decision.selected?.track?.getValidUrl())
        assertNull(decision.fallbackReason)
    }

    @Test
    fun `missing hi res falls back to best standard without changing request`() {
        val decision = resolveAudioStreamSelection(
            dash = Dash(audio = listOf(standard64, standard192)),
            requestedAudioQuality = AUDIO_QUALITY_HI_RES
        )

        assertEquals(AUDIO_QUALITY_HI_RES, decision.requestedPreferenceId)
        assertEquals(AUDIO_QUALITY_AUTO, decision.selectedPreferenceId)
        assertEquals(AudioFallbackReason.REQUESTED_UNAVAILABLE, decision.fallbackReason)
    }

    @Test
    fun `high speed temporarily falls back from hi res to standard`() {
        val decision = resolveAudioStreamSelection(
            dash = fullDash(),
            requestedAudioQuality = AUDIO_QUALITY_HI_RES,
            playbackSpeed = 2.0f
        )

        assertEquals(AUDIO_QUALITY_HI_RES, decision.requestedPreferenceId)
        assertEquals(AUDIO_QUALITY_AUTO, decision.effectivePreferenceId)
        assertEquals(AUDIO_QUALITY_AUTO, decision.selectedPreferenceId)
        assertEquals(AudioFallbackReason.SPEED_INCOMPATIBLE, decision.fallbackReason)
    }

    @Test
    fun `legacy concrete AAC default maps to high quality AAC`() {
        assertEquals(
            AUDIO_QUALITY_AUTO,
            resolveRequestedAudioQuality(
                defaultAudioQuality = 30280,
                rememberedAudioQuality = AUDIO_QUALITY_HI_RES
            )
        )
    }

    @Test
    fun `follow last default uses remembered manual selection`() {
        assertEquals(
            AUDIO_QUALITY_HI_RES,
            resolveRequestedAudioQuality(
                defaultAudioQuality = AUDIO_QUALITY_FOLLOW_LAST_SELECTED,
                rememberedAudioQuality = AUDIO_QUALITY_HI_RES
            )
        )
    }

    @Test
    fun `available options only expose tracks returned by current response`() {
        val options = buildAvailableAudioQualityOptions(
            collectAudioStreamCandidates(
                Dash(audio = listOf(standard192))
            )
        )

        assertEquals(listOf(AUDIO_QUALITY_AUTO), options.map { it.preferenceId })
        assertEquals("AAC", options.single().label)
        assertTrue(options.none { it.isHiRes })
        assertTrue(options.none { it.isDolby })
    }

    @Test
    fun `dolby container does not promote AAC track to dolby`() {
        val aacInDolbyContainer = standard192.copy(
            baseUrl = "https://example.com/audio-aac-in-dolby-container.m4s",
            codecs = "mp4a.40.2"
        )

        val candidates = collectAudioStreamCandidates(
            Dash(
                audio = listOf(standard192),
                dolby = Dolby(type = 1, audio = listOf(aacInDolbyContainer))
            )
        )

        assertTrue(candidates.none { it.kind == AudioStreamKind.DOLBY })
    }

    @Test
    fun `dolby track requires both dolby id and E AC 3 codec`() {
        val dolbyIdWithAacCodec = dolby.copy(codecs = "mp4a.40.2")
        val wrongIdWithDolbyCodec = dolby.copy(id = 30280)

        val candidates = collectAudioStreamCandidates(
            Dash(
                dolby = Dolby(
                    type = 1,
                    audio = listOf(dolbyIdWithAacCodec, wrongIdWithDolbyCodec)
                )
            )
        )

        assertTrue(candidates.none { it.kind == AudioStreamKind.DOLBY })
    }

    @Test
    fun `disabled dolby container does not expose dolby track`() {
        val candidates = collectAudioStreamCandidates(
            Dash(
                dolby = Dolby(type = 0, audio = listOf(dolby))
            )
        )

        assertTrue(candidates.none { it.kind == AudioStreamKind.DOLBY })
    }

    @Test
    fun `unsupported dolby decoder hides dolby and falls back to AAC`() {
        val decision = resolveAudioStreamSelection(
            dash = fullDash(),
            requestedAudioQuality = AUDIO_QUALITY_DOLBY,
            isDolbyAudioSupported = false
        )

        assertEquals(AUDIO_QUALITY_AUTO, decision.selectedPreferenceId)
        assertEquals(AudioStreamKind.STANDARD, decision.selected?.kind)
        assertEquals(AudioFallbackReason.REQUESTED_UNAVAILABLE, decision.fallbackReason)
        assertTrue(decision.availableOptions.none { it.isDolby })
    }

    @Test
    fun `premium options expose matching audio badges`() {
        val options = buildAvailableAudioQualityOptions(
            collectAudioStreamCandidates(fullDash())
        )

        val hiResOption = options.first { it.preferenceId == AUDIO_QUALITY_HI_RES }
        val dolbyOption = options.first { it.preferenceId == AUDIO_QUALITY_DOLBY }
        val aacOption = options.first { it.preferenceId == AUDIO_QUALITY_AUTO }

        assertEquals(
            listOf(AUDIO_QUALITY_HI_RES, AUDIO_QUALITY_DOLBY, AUDIO_QUALITY_AUTO),
            options.map { it.preferenceId }
        )
        assertTrue(hiResOption.isHiRes)
        assertTrue(!hiResOption.isDolby)
        assertTrue(dolbyOption.isDolby)
        assertTrue(!dolbyOption.isHiRes)
        assertEquals(AudioStreamKind.STANDARD, aacOption.kind)
    }

    @Test
    fun `software decoded dolby uses dolby audio label`() {
        val decision = resolveAudioStreamSelection(
            dash = fullDash(),
            requestedAudioQuality = AUDIO_QUALITY_DOLBY,
            isDolbyAudioSupported = true,
            isDolbyAudioSoftwareDecoded = true
        )

        val dolbyOption = decision.availableOptions.first { it.preferenceId == AUDIO_QUALITY_DOLBY }
        val presentation = resolveAudioQualityControlPresentation(
            options = decision.availableOptions,
            selectedAudioQuality = AUDIO_QUALITY_DOLBY
        )

        assertEquals("杜比音频", dolbyOption.label)
        assertTrue(dolbyOption.isSoftwareDecoded)
        assertEquals("杜比音频", presentation.label)
        assertTrue(presentation.showDolbyBadge)
    }

    @Test
    fun `platform decoded dolby keeps atmos label`() {
        val decision = resolveAudioStreamSelection(
            dash = fullDash(),
            requestedAudioQuality = AUDIO_QUALITY_DOLBY,
            isDolbyAudioSupported = true,
            isDolbyAudioSoftwareDecoded = false
        )

        val dolbyOption = decision.availableOptions.first { it.preferenceId == AUDIO_QUALITY_DOLBY }
        assertEquals("杜比全景声", dolbyOption.label)
        assertTrue(!dolbyOption.isSoftwareDecoded)
    }

    @Test
    fun `audio quality control keeps a visible fallback when options are empty`() {
        val presentation = resolveAudioQualityControlPresentation(
            options = emptyList(),
            selectedAudioQuality = AUDIO_QUALITY_AUTO
        )

        assertEquals("音质", presentation.label)
        assertTrue(!presentation.showHiResBadge)
        assertTrue(!presentation.showDolbyBadge)
    }

    @Test
    fun `single AAC option remains visible in audio quality control`() {
        val options = buildAvailableAudioQualityOptions(
            collectAudioStreamCandidates(Dash(audio = listOf(standard192)))
        )

        val presentation = resolveAudioQualityControlPresentation(
            options = options,
            selectedAudioQuality = AUDIO_QUALITY_AUTO
        )

        assertEquals("AAC", presentation.label)
    }

    @Test
    fun `Hi Res control shows explicit text next to badge`() {
        val options = buildAvailableAudioQualityOptions(
            collectAudioStreamCandidates(fullDash())
        )

        val presentation = resolveAudioQualityControlPresentation(
            options = options,
            selectedAudioQuality = AUDIO_QUALITY_HI_RES
        )

        assertEquals("Hi-Res", presentation.label)
        assertTrue(presentation.showHiResBadge)
    }

    private fun fullDash(): Dash {
        return Dash(
            audio = listOf(standard192, standard64),
            dolby = Dolby(type = 1, audio = listOf(dolby)),
            flac = Flac(display = true, audio = hiRes)
        )
    }
}
