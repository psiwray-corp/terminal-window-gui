package com.psiwray.organization.terminal_window_gui.examples.todo

import com.psiwray.organization.terminal_window.TerminalWindow
import com.psiwray.organization.terminal_window.input.Key
import com.psiwray.organization.terminal_window.input.Modifiers
import com.psiwray.organization.terminal_window_gui.GuiSurface
import com.psiwray.organization.terminal_window_gui.command.Command
import com.psiwray.organization.terminal_window_gui.command.KeyChord
import com.psiwray.organization.terminal_window_gui.examples.ExampleApp
import com.psiwray.organization.terminal_window_gui.examples.ExampleLoop
import com.psiwray.organization.terminal_window_gui.widget.AppShell
import com.psiwray.organization.terminal_window_gui.widget.Button
import com.psiwray.organization.terminal_window_gui.widget.CheckState
import com.psiwray.organization.terminal_window_gui.widget.Checkbox
import com.psiwray.organization.terminal_window_gui.widget.Column
import com.psiwray.organization.terminal_window_gui.widget.ConfirmDialog
import com.psiwray.organization.terminal_window_gui.widget.Expanded
import com.psiwray.organization.terminal_window_gui.widget.Label
import com.psiwray.organization.terminal_window_gui.widget.ListView
import com.psiwray.organization.terminal_window_gui.widget.Menu
import com.psiwray.organization.terminal_window_gui.widget.MenuBar
import com.psiwray.organization.terminal_window_gui.widget.MenuItem
import com.psiwray.organization.terminal_window_gui.widget.Panel
import com.psiwray.organization.terminal_window_gui.widget.PromptDialog
import com.psiwray.organization.terminal_window_gui.widget.Row
import com.psiwray.organization.terminal_window_gui.widget.SelectionMode
import com.psiwray.organization.terminal_window_gui.widget.SelectionModel
import com.psiwray.organization.terminal_window_gui.widget.StatusBar
import com.psiwray.organization.terminal_window_gui.widget.TextField
import com.psiwray.organization.terminal_window_gui.widget.Widget

/** Entry point for the TODO example. */
public fun main(args: Array<String>) {
    ExampleLoop.run(TodoApp(), args)
}

private data class Task(
    val id: Int,
    var title: String,
    var done: Boolean,
)

private class TodoApp : ExampleApp("terminal-window-gui | TODO", columns = 80, rows = 24) {
    private lateinit var surface: GuiSurface
    private lateinit var window: TerminalWindow
    private lateinit var list: ListView<Task>
    private lateinit var status: StatusBar
    private lateinit var input: TextField
    private val selection = SelectionModel(SelectionMode.SINGLE)
    private val tasks =
        mutableListOf(
            Task(1, "Review section 13", done = true),
            Task(2, "Run the example smoke gate", done = false),
            Task(3, "Capture gallery screenshots", done = false),
            Task(4, "Document keyboard shortcuts", done = false),
        )
    private var nextId = 5

    override fun configure(
        surface: GuiSurface,
        window: TerminalWindow,
    ) {
        this.surface = surface
        this.window = window
        surface.keymap.bind(KeyChord(Key.N, Modifiers.CONTROL), Command("new", action = { newTask() }))
        surface.keymap.bind(KeyChord(Key.E, Modifiers.CONTROL), Command("edit", action = { editTask() }))
        surface.keymap.bind(KeyChord(Key.DELETE), Command("delete", action = { deleteTask() }))
        surface.keymap.bind(KeyChord(Key.SPACE, Modifiers.CONTROL), Command("toggle", action = { toggleTask() }))
    }

    override fun root(): Widget {
        list =
            ListView(
                items = tasks.toList(),
                keyOf = { it.id },
                selection = selection,
                onActivate = { editTask(it) },
                itemBuilder = { _, task ->
                    Checkbox(
                        label = task.title,
                        state = if (task.done) CheckState.CHECKED else CheckState.UNCHECKED,
                        onChanged = { state ->
                            task.done = state == CheckState.CHECKED
                            updateStatus()
                        },
                    )
                },
            )
        input = TextField(placeholder = "New task title")
        status = StatusBar(left = "TODO", right = "")
        updateStatus()
        val content =
            Column(
                gap = 1,
                children =
                    listOf(
                        Label("A small task list. Enter edits; Ctrl+N adds; Delete removes; Space toggles."),
                        Row(
                            gap = 1,
                            children =
                                listOf(
                                    Expanded(child = input),
                                    Button(
                                        label = "Add",
                                        default = true,
                                        onActivate = {
                                            addTask(input.controller.text)
                                            input.controller.setText("")
                                        },
                                    ),
                                    Button(label = "Edit", onActivate = { editTask() }),
                                    Button(label = "Delete", onActivate = { deleteTask() }),
                                ),
                        ),
                        Expanded(child = Panel(title = "Tasks", child = list)),
                    ),
            )
        return AppShell(menuBar = buildMenu(), content = content, statusBar = status)
    }

    private fun buildMenu(): MenuBar {
        val new = Command("new", "New task", shortcut = KeyChord(Key.N, Modifiers.CONTROL), action = { newTask() })
        val edit = Command("edit", "Edit task", shortcut = KeyChord(Key.E, Modifiers.CONTROL), action = { editTask() })
        val delete = Command("delete", "Delete task", shortcut = KeyChord(Key.DELETE), action = { deleteTask() })
        val toggle =
            Command(
                "toggle",
                "Toggle done",
                shortcut = KeyChord(Key.SPACE, Modifiers.CONTROL),
                action = { toggleTask() },
            )
        val quit =
            Command("quit", "Quit", shortcut = KeyChord(Key.Q, Modifiers.CONTROL), action = { window.requestClose() })
        return MenuBar(
            Menu(
                "File",
                'F',
                listOf(
                    MenuItem.CommandItem(new),
                    MenuItem.Separator,
                    MenuItem.CommandItem(quit),
                ),
            ),
            Menu(
                "Task",
                'T',
                listOf(
                    MenuItem.CommandItem(edit),
                    MenuItem.CommandItem(delete),
                    MenuItem.CommandItem(toggle),
                ),
            ),
        )
    }

    private fun selectedTask(): Task? {
        val key = selection.selectedKeys.firstOrNull() ?: return tasks.firstOrNull()
        return tasks.firstOrNull { it.id == key }
    }

    private fun newTask() {
        PromptDialog("New task", "Task title", result = { value -> addTask(value) }).show(surface.overlays)
    }

    private fun addTask(value: String?) {
        val title = value?.trim().orEmpty()
        if (title.isEmpty()) {
            return
        }
        val task = Task(nextId++, title, done = false)
        tasks += task
        refreshList()
        selection.select(task.id)
        updateStatus()
    }

    private fun editTask(task: Task? = selectedTask()) {
        val target = task ?: return
        PromptDialog(
            "Edit task",
            "Task title",
            initialValue = target.title,
            result = { value ->
                val title = value?.trim().orEmpty()
                if (title.isNotEmpty()) {
                    target.title = title
                    refreshList()
                    updateStatus()
                }
            },
        ).show(surface.overlays)
    }

    private fun deleteTask() {
        val task = selectedTask() ?: return
        ConfirmDialog(
            "Delete task",
            "Delete \"${task.title}\"?",
            result = { confirmed ->
                if (confirmed) {
                    tasks.remove(task)
                    refreshList()
                    updateStatus()
                }
            },
        ).show(surface.overlays)
    }

    private fun toggleTask() {
        val task = selectedTask() ?: return
        task.done = !task.done
        refreshList()
        updateStatus()
    }

    private fun refreshList() {
        list.items = tasks.toList()
        list.invalidateLayout()
    }

    private fun updateStatus() {
        status.right = "${tasks.count { it.done }}/${tasks.size} done | Ctrl+Q quit"
    }
}
