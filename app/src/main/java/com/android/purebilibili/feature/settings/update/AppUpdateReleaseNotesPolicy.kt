package com.android.purebilibili.feature.settings

private const val EMPTY_RELEASE_NOTES_PLACEHOLDER = "暂无更新说明"

internal sealed interface AppUpdateReleaseNotesBlock {
    data class Heading(val text: String, val level: Int) : AppUpdateReleaseNotesBlock
    data class Bullet(val text: String, val ordered: Boolean) : AppUpdateReleaseNotesBlock
    data object Divider : AppUpdateReleaseNotesBlock
    data class Paragraph(val text: String) : AppUpdateReleaseNotesBlock
}

internal fun resolveUpdateReleaseNotesText(releaseNotes: String): String {
    return releaseNotes.trim().ifBlank { EMPTY_RELEASE_NOTES_PLACEHOLDER }
}

internal fun parseUpdateReleaseNotes(releaseNotes: String): List<AppUpdateReleaseNotesBlock> {
    val normalized = resolveUpdateReleaseNotesText(releaseNotes)
    return normalized.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map { line ->
            when {
                line.matches(Regex("^#{1,2}\\s+.+")) -> {
                    val level = line.takeWhile { it == '#' }.length
                    AppUpdateReleaseNotesBlock.Heading(line.drop(level).trim(), level)
                }
                line == "---" || line == "***" || line == "___" -> AppUpdateReleaseNotesBlock.Divider
                line.matches(Regex("^[-*+]\\s+.+")) -> AppUpdateReleaseNotesBlock.Bullet(
                    text = line.drop(1).trim(),
                    ordered = false
                )
                line.matches(Regex("^\\d+[.)]\\s+.+")) -> AppUpdateReleaseNotesBlock.Bullet(
                    text = line.replaceFirst(Regex("^\\d+[.)]\\s+"), ""),
                    ordered = true
                )
                else -> AppUpdateReleaseNotesBlock.Paragraph(line)
            }
        }
        .toList()
}
