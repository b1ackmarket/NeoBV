package dev.aaa1115910.bv.resource

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeExitMessageTest {
    @Test
    fun `home exit prompt uses NeoBee Video name`() {
        val stringsFile = File("src/main/res/values/strings.xml")
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(stringsFile)
        val strings = document.getElementsByTagName("string")

        val value = (0 until strings.length)
            .asSequence()
            .map { strings.item(it) }
            .first { it.attributes.getNamedItem("name").nodeValue == "home_press_back_again_to_exit" }
            .textContent

        assertEquals("再次按下返回键退出NeoBee Video", value)
    }
}
