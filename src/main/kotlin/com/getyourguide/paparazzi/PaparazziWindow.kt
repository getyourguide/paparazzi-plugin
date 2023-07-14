package com.getyourguide.paparazzi

import com.getyourguide.paparazzi.actions.DeleteFileAction
import com.getyourguide.paparazzi.actions.OpenFileAction
import com.getyourguide.paparazzi.service.HORIZONTAL_PADDING
import com.getyourguide.paparazzi.service.Snapshot
import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH
import com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED
import com.intellij.uiDesigner.core.GridLayoutManager
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Insets
import java.awt.Point
import java.awt.Rectangle
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

private const val GROUP_TOOLBAR = "com.getyourguide.paparazzi.toolbar"
const val TOOL_WINDOW_ID = "Paparazzi"

class PaparazziWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        PaparazziWindowPanelImpl(toolWindow, project)
    }
}

internal class PaparazziWindowPanelImpl(toolWindow: ToolWindow, project: Project) : PaparazziWindowPanel() {

    private val model = project.service.model
    override val list = object : JBList<Snapshot>(model) {
        override fun getScrollableUnitIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int): Int = 30
    }

    init {
        val content = ContentFactory.getInstance().createContent(this, "", false)
        toolWindow.contentManager.addContent(content)
        list.cellRenderer = Renderer(project)
        list.addMouseListener(ContextMenuHandler(list, ActionManager.getInstance()))

        setContent(getContentPanel())
        project.service.panel = this
    }

    private fun getContentPanel(): JPanel {

        val panel = JBPanel<SimpleToolWindowPanel>(
            GridLayoutManager(2, 1, Insets(0, 0, 0, 0), 0, 0)
        )

        val toolbar = JPanel(BorderLayout())
        initToolbar(toolbar)

        panel.add(toolbar, GridConstraints().apply {
            row = 0
            fill = FILL_BOTH
            vSizePolicy = SIZEPOLICY_FIXED
        })

        panel.add(
            JBScrollPane(list, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED),
            GridConstraints().apply {
                row = 1
                fill = FILL_BOTH
            })

        return panel
    }

    private fun initToolbar(toolbar: JPanel) {
        val manager = ActionManager.getInstance()
        val actionGroup = manager.getAction(GROUP_TOOLBAR) as ActionGroup
        val actionToolbar = manager.createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, actionGroup, true)
        actionToolbar.targetComponent = toolbar
        toolbar.add(actionToolbar.component)
    }
}

abstract class PaparazziWindowPanel : SimpleToolWindowPanel(true, true) {

    abstract val list: JBList<Snapshot>
}

internal class Renderer(private val project: Project) : ListCellRenderer<Snapshot> {
    override fun getListCellRendererComponent(
        list: JList<out Snapshot>?, value: Snapshot, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): Component {
        val image = project.service.image(value)
        val title = JLabel(value.name).apply {
            border = BorderFactory.createEmptyBorder(0, 0, 8, 0)
        }
        val snapshot = if (image != null) {
            JLabel(ImageIcon(image))
        } else {
            JLabel("Unable to load the Snapshot")
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(32, HORIZONTAL_PADDING, 32, HORIZONTAL_PADDING)
            add(title)
            add(snapshot)
        }
    }
}

private class ContextMenuHandler(
    val list: JBList<Snapshot>,
    val actionManager: ActionManager
) : PopupHandler() {
    override fun invokePopup(comp: Component?, x: Int, y: Int) {
        val index = list.locationToIndex(Point(x, y))
        val model = list.model
        if (index < model.size) {
            val snapshot = model.getElementAt(index)
            val actionGroup = DefaultActionGroup(
                OpenFileAction(snapshot.file),
                DeleteFileAction(snapshot.file)
            )
            val popupMenu = actionManager.createActionPopupMenu(ActionPlaces.POPUP, actionGroup)
            popupMenu.setTargetComponent(list)
            popupMenu.component.show(comp, x, y)
        }
    }
}
