package com.android.purebilibili.feature.settings

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class ThemeContrastStringResourceTest {
    @Test
    fun contrastIssueFormatsLabelAndFloatArgumentsInEachTranslation() {
        for (directory in listOf("values", "values-en")) {
            val format = readString(directory, "appearance_theme_contrast_issue_item")
            for (locale in listOf(Locale.SIMPLIFIED_CHINESE, Locale.US)) {
                val label = "主要文字"
                val result = String.format(locale, format, label, 2.25f, 4.5f)
                assertTrue(result, result.contains(label))
                assertTrue(result, result.contains("2.25:1"))
                assertTrue(result, result.contains("4.5:1"))
            }
        }
    }

    private fun readString(directory: String, name: String): String {
        val resource = listOf(File("src/main/res"), File("app/src/main/res"))
            .map { File(it, "$directory/strings.xml") }
            .first { it.isFile }
        val strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(resource).getElementsByTagName("string")
        for (index in 0 until strings.length) {
            val element = strings.item(index) as Element
            if (element.getAttribute("name") == name) return element.textContent
        }
        error("Missing string $name in $resource")
    }
}
