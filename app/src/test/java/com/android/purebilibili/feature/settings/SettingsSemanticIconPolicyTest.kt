package com.android.purebilibili.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.theme.UiPreset
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChartBar
import io.github.alexzhirkevich.cupertino.icons.outlined.DocOnDoc
import io.github.alexzhirkevich.cupertino.icons.outlined.House
import io.github.alexzhirkevich.cupertino.icons.outlined.TextBubble
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SettingsSemanticIconPolicyTest {

    @Test
    fun homeFeedEntry_usesHomeSemanticIconInsteadOfAnalyticsIcon() {
        val icon = resolveSettingsSemanticIcon(SettingsIconRole.HOME_FEED, UiPreset.IOS)

        assertSameVectorAsset(CupertinoIcons.Outlined.House, icon)
        assertNotEquals(CupertinoIcons.Default.ChartBar.name, icon.name)
    }

    @Test
    fun md3HomeFeedEntry_usesMaterialHomeSemanticIcon() {
        assertSameVectorAsset(
            Icons.Outlined.Home,
            resolveSettingsSemanticIcon(SettingsIconRole.HOME_FEED, UiPreset.MD3)
        )
    }

    @Test
    fun settingsSceneRoles_useConcreteDomainIcons() {
        assertSameVectorAsset(
            CupertinoIcons.Outlined.TextBubble,
            resolveSettingsSemanticIcon(SettingsIconRole.INTERACTION_COMMENT, UiPreset.IOS)
        )
        assertSameVectorAsset(
            CupertinoIcons.Outlined.DocOnDoc,
            resolveSettingsSemanticIcon(SettingsIconRole.DATA_BACKUP, UiPreset.IOS)
        )
        assertSameVectorAsset(
            Icons.Outlined.Terminal,
            resolveSettingsSemanticIcon(SettingsIconRole.DIAGNOSTICS, UiPreset.MD3)
        )
    }

    @Test
    fun md3Preset_usesUniqueIconForEverySettingsRole() {
        assertSettingsRoleIconsAreUnique(UiPreset.MD3)
    }

    @Test
    fun iosPreset_usesUniqueIconForEverySettingsRole() {
        assertSettingsRoleIconsAreUnique(UiPreset.IOS)
    }

    @Test
    fun visibleSettingsGroups_doNotReuseTheSameSemanticRole() {
        val duplicateMessages = settingsSourceFiles()
            .flatMap { file -> duplicatedRolesInsideVisibleGroups(file) }

        assertTrue(
            duplicateMessages.isEmpty(),
            duplicateMessages.joinToString(separator = "\n")
        )
    }

    private fun assertSettingsRoleIconsAreUnique(uiPreset: UiPreset) {
        val duplicates = SettingsIconRole.entries
            .groupBy { role -> resolveSettingsSemanticIcon(role, uiPreset).assetKey() }
            .filterValues { roles -> roles.size > 1 }

        assertTrue(
            duplicates.isEmpty(),
            duplicates.entries.joinToString(separator = "\n") { (assetKey, roles) ->
                "$uiPreset duplicate $assetKey: ${roles.joinToString { it.name }}"
            }
        )
    }

    private fun settingsSourceFiles(): List<File> {
        val roots = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen"),
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui")
        )
        return roots.flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        }
    }

    private fun duplicatedRolesInsideVisibleGroups(file: File): List<String> {
        val lines = file.readLines()
        val messages = mutableListOf<String>()
        var groupStartLine: Int? = null
        var groupBraceDepth = 0
        val groupRoles = mutableListOf<Pair<Int, String>>()

        lines.forEachIndexed { index, line ->
            if (groupStartLine == null && (line.contains("IOSGroup {") || line.contains("SettingsCardGroup {"))) {
                groupStartLine = index + 1
                groupBraceDepth = line.braceDelta()
                groupRoles.clear()
            }

            val activeGroupStart = groupStartLine
            if (activeGroupStart != null) {
                ROLE_USAGE_REGEX.findAll(line).forEach { match ->
                    groupRoles += (index + 1) to match.groupValues[1]
                }

                if (index + 1 != activeGroupStart) {
                    groupBraceDepth += line.braceDelta()
                }
                if (groupBraceDepth <= 0) {
                    groupRoles
                        .groupBy { it.second }
                        .filterValues { usages -> usages.size > 1 }
                        .forEach { (role, usages) ->
                            messages += "${file.path}:$activeGroupStart ${role} reused at lines ${
                                usages.joinToString { it.first.toString() }
                            }"
                    }
                    groupStartLine = null
                    groupBraceDepth = 0
                    groupRoles.clear()
                }
            }
        }

        return messages
    }

    private fun String.braceDelta(): Int = count { it == '{' } - count { it == '}' }

    private fun assertSameVectorAsset(expected: ImageVector, actual: ImageVector) {
        assertEquals(expected.name, actual.name)
        assertEquals(expected.defaultWidth, actual.defaultWidth)
        assertEquals(expected.defaultHeight, actual.defaultHeight)
        assertEquals(expected.viewportWidth, actual.viewportWidth)
        assertEquals(expected.viewportHeight, actual.viewportHeight)
    }

    private fun ImageVector.assetKey(): String = listOf(
        name,
        defaultWidth.value,
        defaultHeight.value,
        viewportWidth,
        viewportHeight
    ).joinToString(separator = "|")

    private companion object {
        val ROLE_USAGE_REGEX = Regex("""SettingsIconRole\.([A-Z0-9_]+)""")
    }
}
