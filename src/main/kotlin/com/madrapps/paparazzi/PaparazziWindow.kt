package com.madrapps.paparazzi

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.*
import com.intellij.uiDesigner.core.GridLayoutManager
import com.madrapps.paparazzi.actions.*
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import java.awt.BorderLayout
import java.awt.Image
import java.awt.Insets
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants.*

class PaparazziWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        MyPanel(toolWindow, project)
    }
}

class MyPanel(private val toolWindow: ToolWindow, project: Project) : SimpleToolWindowPanel(true, true) {

    init {
        val content = ContentFactory.SERVICE.getInstance().createContent(this, "", false)
        toolWindow.contentManager.addContent(content)
        setContent(getContentPanel(project))
    }

    private fun getContentPanel(project: Project): JPanel {
        val panel = JBPanel<SimpleToolWindowPanel>(
            GridLayoutManager(
                2,
                1,
                Insets(0, 0, 0, 0),
                0,
                0
            )
        )

        val toolbar = JPanel(BorderLayout())
        initToolbar(toolbar)
        panel.add(toolbar, GridConstraints().apply {
            row = 0
            fill = FILL_BOTH
            vSizePolicy = SIZEPOLICY_FIXED
        })

        val jPanel = JPanel()
        val boxLayout = BoxLayout(jPanel, BoxLayout.Y_AXIS)
        jPanel.layout = boxLayout
        jPanel.border = BorderFactory.createEmptyBorder(32, 32, 32, 32)

        val modules = project.allModules()
        val sourceRoots = modules[0].rootManager.sourceRoots

        val children = modules[90].rootManager.contentRoots[0].children[1].children[0].children

        children.take(25).forEach { child ->
            val read = ImageIO.read(child.inputStream)
            val width = read.width.toFloat()
            val height = read.height.toFloat()
            val newHeight = (height/width*300).toInt()
            val scaledInstance = read.getScaledInstance(300, newHeight, Image.SCALE_SMOOTH)
            val jLabel = JLabel(ImageIcon(scaledInstance))
            jLabel.border = BorderFactory.createEmptyBorder(32, 32, 32, 32)
            jPanel.add(jLabel)
        }

        val jbScrollPane = JBScrollPane(
            jPanel,
            VERTICAL_SCROLLBAR_AS_NEEDED,
            HORIZONTAL_SCROLLBAR_AS_NEEDED
        )
        panel.add(jbScrollPane, GridConstraints().apply {
            row = 1
            fill = FILL_BOTH
        })

        return panel
    }

    private fun initToolbar(toolbar: JPanel) {
        val manager = ActionManager.getInstance()
        val refreshAction = manager.getAction(RefreshAction.ID)
        val zoomInAction = manager.getAction(ZoomInAction.ID)
        val zoomOutAction = manager.getAction(ZoomOutAction.ID)
        val actualSizeAction = manager.getAction(ActualSizeAction.ID)
        val fitZoomToWindowAction = manager.getAction(FitZoomToWindowAction.ID)

        val toolbarActionGroup = DefaultActionGroup().apply {
            add(refreshAction)
            addSeparator()
            add(zoomInAction)
            add(zoomOutAction)
            add(actualSizeAction)
            add(fitZoomToWindowAction)
        }

        val actionToolbar = manager.createActionToolbar(ActionPlaces.TOOLWINDOW_TITLE, toolbarActionGroup, true)
        actionToolbar.targetComponent = toolbar
        toolbar.add(actionToolbar.component)
    }
}
