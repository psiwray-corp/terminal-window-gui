package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import com.psiwray.organization.terminal_window_gui.text.TextEditingController
import kotlin.test.Test

class TextEditingGoldenTest {
    @Test
    fun textFieldGolden() {
        val controller = TextEditingController("hello")
        val field = TextField(controller, placeholder = "type")
        val harness = GuiTestHarness(field, columns = 10, rows = 1)
        harness.surface.focus.focus(field)
        controller.selection =
            com.psiwray.organization.terminal_window_gui.text
                .TextSelection(1, 4)
        harness.paint()
        GoldenAssert.assertRows("text-field.txt", harness.rows())
    }

    @Test
    fun textAreaGolden() {
        val controller = TextEditingController("alpha beta\ngamma")
        val area =
            TextArea(
                controller,
                wrap = TextAreaWrap.WORD,
                lineNumbers = LineNumbers(enabled = true, minDigits = 2),
            )
        val harness = GuiTestHarness(area, columns = 12, rows = 4)
        harness.surface.focus.focus(area)
        controller.textEnd()
        harness.paint()
        GoldenAssert.assertRows("text-area.txt", harness.rows())
    }

    @Test
    fun emptyWidgetsAtOneCellGolden() {
        val field = GuiTestHarness(TextField(), columns = 1, rows = 1)
        field.paint()
        GoldenAssert.assertRows("text-field-empty-1x1.txt", field.rows())
        val area = GuiTestHarness(TextArea(), columns = 1, rows = 1)
        area.paint()
        GoldenAssert.assertRows("text-area-empty-1x1.txt", area.rows())
        val log = GuiTestHarness(LogView(), columns = 1, rows = 1)
        log.paint()
        GoldenAssert.assertRows("log-view-empty-1x1.txt", log.rows())
    }

    @Test
    fun textAreaAtEightyByTwentyFourGolden() {
        val controller = TextEditingController("one two three four five six seven eight nine ten")
        val area = TextArea(controller, wrap = TextAreaWrap.WORD, lineNumbers = true)
        val harness = GuiTestHarness(area, columns = 80, rows = 24)
        harness.paint()
        GoldenAssert.assertRows("text-area-80x24.txt", harness.rows())
    }

    @Test
    fun logViewGolden() {
        val view = LogView(listOf("build", "test ok", "release"))
        val harness = GuiTestHarness(view, columns = 12, rows = 3)
        harness.paint()
        GoldenAssert.assertRows("log-view.txt", harness.rows())
    }
}
