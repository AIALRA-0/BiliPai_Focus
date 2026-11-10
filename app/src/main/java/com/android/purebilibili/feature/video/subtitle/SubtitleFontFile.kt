package com.android.purebilibili.feature.video.subtitle

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val MAX_SUBTITLE_FONT_BYTES = 32L * 1024 * 1024

/** 字幕使用独立目录，校验成功后才发布文件，导入失败保留原字体。 */
suspend fun importSubtitleFont(context: Context, uri: Uri): Pair<String, String> = withContext(Dispatchers.IO) {
    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: uri.lastPathSegment.orEmpty()
    val extension = name.substringAfterLast('.', "").lowercase()
    require(extension in setOf("ttf", "otf", "ttc")) { "请选择 TTF、OTF 或 TTC 字体文件" }
    val directory = File(context.filesDir, "subtitle_fonts").apply { mkdirs() }
    val temporary = File.createTempFile("import-", ".tmp", directory)
    try {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取字体文件" }
            temporary.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_SUBTITLE_FONT_BYTES) { "字体文件不能超过 32 MB" }
                    output.write(buffer, 0, count)
                }
                require(total > 0) { "字体文件为空" }
            }
        }
        Typeface.createFromFile(temporary)
        val target = File(directory, "${UUID.randomUUID()}.$extension")
        check(temporary.renameTo(target)) { "无法保存字幕字体" }
        target.name to name
    } finally {
        temporary.delete()
    }
}

internal fun loadSubtitleFontFamily(context: Context, fileName: String): FontFamily? {
    if (fileName.isBlank() || fileName != File(fileName).name) return null
    val file = File(File(context.filesDir, "subtitle_fonts"), fileName)
    if (!file.isFile) return null
    return runCatching { FontFamily(Typeface.createFromFile(file)) }.getOrNull()
}
