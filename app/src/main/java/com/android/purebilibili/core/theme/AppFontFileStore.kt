package com.android.purebilibili.core.theme

import android.content.Context
import android.database.Cursor
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.util.Locale

private const val APP_FONT_DIR_NAME = "app_fonts"
private const val APP_FONT_FILE_PREFIX = "custom_app_font"
private val APP_FONT_ALLOWED_EXTENSIONS = setOf("ttf", "otf", "ttc")

data class ImportedAppFontFile(
    val fileName: String,
    val displayName: String
)

internal fun sanitizeAppFontDisplayName(name: String?): String {
    val trimmed = name?.trim().orEmpty()
    return trimmed.ifBlank { "本地字体" }
}

internal fun resolveAppFontExtension(displayName: String?): String {
    val extension = displayName
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase(Locale.ROOT)
        .orEmpty()
    return extension.takeIf { it in APP_FONT_ALLOWED_EXTENSIONS } ?: "ttf"
}

internal fun buildStoredAppFontFileName(displayName: String?): String {
    return "$APP_FONT_FILE_PREFIX.${resolveAppFontExtension(displayName)}"
}

fun resolveStoredAppFontFile(context: Context, fileName: String): File? {
    val normalizedName = fileName.substringAfterLast('/').trim()
    if (normalizedName.isBlank()) return null
    return File(File(context.filesDir, APP_FONT_DIR_NAME), normalizedName)
        .takeIf { it.exists() && it.isFile }
}

fun loadStoredAppFontFamily(context: Context, fileName: String): FontFamily? {
    val fontFile = resolveStoredAppFontFile(context, fileName) ?: return null
    return runCatching {
        FontFamily(Typeface.createFromFile(fontFile))
    }.getOrNull()
}

fun importAppFontFromUri(context: Context, uri: Uri): Result<ImportedAppFontFile> {
    return runCatching {
        val displayName = sanitizeAppFontDisplayName(queryDisplayName(context, uri))
        val storedFileName = buildStoredAppFontFileName(displayName)
        val fontDir = File(context.filesDir, APP_FONT_DIR_NAME).apply { mkdirs() }
        val targetFile = File(fontDir, storedFileName)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取字体文件" }
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }

        // 先加载一次，避免把非字体文件保存为全局字体后导致后续启动回退。
        Typeface.createFromFile(targetFile)
        ImportedAppFontFile(
            fileName = storedFileName,
            displayName = displayName
        )
    }
}

fun deleteStoredAppFont(context: Context, fileName: String) {
    resolveStoredAppFontFile(context, fileName)?.delete()
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    val cursor: Cursor? = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )
    return cursor.use {
        if (it != null && it.moveToFirst()) {
            it.getString(0)
        } else {
            uri.lastPathSegment
        }
    }
}
