package com.getyourguide.paparazzi.service

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel


class ProjectSettingsConfigurable(val project: Project) : Configurable {

    private val component = ProjectSettingsComponent()

    override fun createComponent(): JComponent? {
        return component.getPanel()
    }

    override fun isModified(): Boolean {
        return component.recordCommand.text != project.service.settings.recordSnapshotsCommand ||
                component.recordScriptParams.text != project.service.settings.recordScriptParams ||
                component.verifyCommand.text != project.service.settings.verifySnapshotsCommand ||
                component.verifyScriptParams.text != project.service.settings.verifyScriptParams
    }

    override fun reset() {
        component.recordCommand.text = project.service.settings.recordSnapshotsCommand
        component.recordScriptParams.text = project.service.settings.recordScriptParams
        component.verifyCommand.text = project.service.settings.verifySnapshotsCommand
        component.verifyScriptParams.text = project.service.settings.verifyScriptParams
    }

    override fun apply() {
        project.service.settings.recordSnapshotsCommand = component.recordCommand.text
        project.service.settings.recordScriptParams = component.recordScriptParams.text
        project.service.settings.verifySnapshotsCommand = component.verifyCommand.text
        project.service.settings.verifyScriptParams = component.verifyScriptParams.text
    }

    override fun getDisplayName(): String = "Paparazzi"
}

private const val GRADLE_COMMAND = "Gradle command"
private const val EXTRA_PARAMS = "Script parameters"

class ProjectSettingsComponent {

    private var myMainPanel: JPanel? = null
    val recordCommand = JBTextField()
    val recordScriptParams = JBTextField()

    val verifyCommand = JBTextField()
    val verifyScriptParams = JBTextField()

    fun getPanel(): JPanel? {
        if (myMainPanel == null) {
            val recordPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel(GRADLE_COMMAND), recordCommand, 0, false)
                .addLabeledComponent(JBLabel(EXTRA_PARAMS), recordScriptParams, 0, false)
                .panel
            recordPanel.border = IdeBorderFactory.createTitledBorder("Record Snapshots")
            val verifyPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel(GRADLE_COMMAND), verifyCommand, 0, false)
                .addLabeledComponent(JBLabel(EXTRA_PARAMS), verifyScriptParams, 0, false)
                .panel
            verifyPanel.border = IdeBorderFactory.createTitledBorder("Verify Snapshots")
            myMainPanel = FormBuilder.createFormBuilder()
                .addComponent(recordPanel)
                .addComponent(verifyPanel)
                .addComponentFillVertically(JPanel(), 0)
                .panel
        }
        return myMainPanel
    }
}
