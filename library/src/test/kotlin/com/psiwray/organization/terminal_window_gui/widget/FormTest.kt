package com.psiwray.organization.terminal_window_gui.widget

import com.psiwray.organization.terminal_window_gui.input.FocusTraversal
import com.psiwray.organization.terminal_window_gui.testkit.GoldenAssert
import com.psiwray.organization.terminal_window_gui.testkit.GuiTestHarness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FormTest {
    @Test
    fun fixedAndFractionLabelWidthsPositionControl() {
        val fixedField = FormField("Name", TextField("x"), helpText = "help")
        val fixed = Form(listOf(fixedField), labelWidth = FormLabelWidth.Fixed(6))
        val fixedHarness = GuiTestHarness(fixed, columns = 20, rows = 4)
        fixedHarness.paint()
        assertEquals(7, fixedField.control.bounds.x)
        assertEquals(13, fixedField.control.bounds.width)
        assertTrue(fixedHarness.rows()[1].startsWith("       help"))

        val fractionField = FormField("Name", TextField("x"))
        val fraction = Form(listOf(fractionField), labelWidth = FormLabelWidth.Fraction(0.25))
        val fractionHarness = GuiTestHarness(fraction, columns = 20, rows = 3)
        fractionHarness.paint()
        assertEquals(6, fractionField.control.bounds.x)
    }

    @Test
    fun contentWidthCapsItselfAndKeepsControl() {
        val field = FormField("Very long label", TextField("v"), labelWidth = FormLabelWidth.Content)
        val form = Form(listOf(field), labelWidth = FormLabelWidth.Content, gap = 0)
        val harness = GuiTestHarness(form, columns = 10, rows = 1)
        harness.paint()
        assertEquals(1, field.control.bounds.width)
        assertEquals(9, field.control.bounds.x)
    }

    @Test
    fun truncationNeverDropsControl() {
        val field = FormField("LongLabel", TextField("v"), labelWidth = FormLabelWidth.Fixed(10))
        val form = Form(listOf(field), labelWidth = FormLabelWidth.Fixed(10), gap = 0)
        val harness = GuiTestHarness(form, columns = 5, rows = 1)
        harness.paint()
        assertEquals(1, field.control.bounds.width)
        assertEquals(4, field.control.bounds.x)
        assertEquals(4, field.control.bounds.width + field.control.bounds.x - 1)
    }

    @Test
    fun helpAndErrorLinesUseControlColumnAndMarker() {
        val helpField = FormField("Name", TextField("x"), helpText = "help")
        val helpForm = Form(listOf(helpField), labelWidth = FormLabelWidth.Fixed(5), gap = 0)
        val helpHarness = GuiTestHarness(helpForm, columns = 20, rows = 2)
        helpHarness.paint()
        assertTrue(helpHarness.rows()[1].contains("help"))

        val errorField = FormField("Name", TextField("x"), errorText = "bad value")
        val errorForm = Form(listOf(errorField), labelWidth = FormLabelWidth.Fixed(5), gap = 0)
        val errorHarness = GuiTestHarness(errorForm, columns = 20, rows = 2)
        errorHarness.paint()
        assertTrue(errorHarness.rows()[1].contains("! bad value"))
        assertTrue(errorHarness.styles()[1].contains("error/surface"))
    }

    @Test
    fun fieldsSeparatedByBlankRowsAndFocusEntersControl() {
        val first = FormField("First", TextField("1"))
        val second = FormField("Second", TextField("2"))
        val form = Form(listOf(first, second), labelWidth = FormLabelWidth.Fixed(6), gap = 1)
        val harness = GuiTestHarness(form, columns = 20, rows = 6)
        harness.paint()
        assertEquals(0, first.bounds.y)
        assertEquals(2, second.bounds.y)
        assertFalse(first.focusable)
        val order = FocusTraversal.order(form)
        assertTrue(order.any { it === first.control })
        assertTrue(order.any { it === second.control })
        assertFalse(order.any { it === first })
    }

    @Test
    fun columnWidthAndFieldAliasAreAccepted() {
        val field = Field("Name", TextField("x"), labelWidth = ColumnWidth.Fixed(4))
        val form = Form(listOf(field), labelWidth = ColumnWidth.Content)
        val harness = GuiTestHarness(form, columns = 20, rows = 2)
        harness.paint()
        assertEquals(5, field.control.bounds.x)
        assertSame(field, form.children.first())
    }

    @Test
    fun oneCellFormKeepsControl() {
        val field = FormField("Label", TextField("x"), helpText = "h", errorText = "e")
        val form = Form(listOf(field), labelWidth = FormLabelWidth.Fixed(4))
        val harness = GuiTestHarness(form, columns = 1, rows = 1)
        harness.paint()
        assertEquals(1, field.control.bounds.width)
        assertEquals(0, field.control.bounds.x)
    }

    @Test
    fun formMatchesGolden() {
        val field =
            FormField(
                "Name",
                TextField("Ada"),
                helpText = "Full name",
                labelWidth = FormLabelWidth.Fixed(8),
            )
        val error =
            FormField(
                "Port",
                TextField("0"),
                errorText = "Must be positive",
                required = true,
                labelWidth = FormLabelWidth.Fixed(8),
            )
        val form = Form(listOf(field, error), labelWidth = FormLabelWidth.Fixed(8))
        val harness = GuiTestHarness(form, columns = 40, rows = 6)
        harness.paint()
        GoldenAssert.assertRows("form-40x6.txt", harness.rows())
    }
}
