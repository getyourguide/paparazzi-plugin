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
                component.recordExtraParams.text != project.service.settings.recordExtraParams ||
                component.verifyCommand.text != project.service.settings.verifySnapshotsCommand ||
                component.verifyExtraParams.text != project.service.settings.verifyExtraParams
    }

    override fun reset() {
        component.recordCommand.text = project.service.settings.recordSnapshotsCommand
        component.recordExtraParams.text = project.service.settings.recordExtraParams
        component.verifyCommand.text = project.service.settings.verifySnapshotsCommand
        component.verifyExtraParams.text = project.service.settings.verifyExtraParams
    }

    override fun apply() {
        project.service.settings.recordSnapshotsCommand = component.recordCommand.text
        project.service.settings.recordExtraParams = component.recordExtraParams.text
        project.service.settings.verifySnapshotsCommand = component.verifyCommand.text
        project.service.settings.verifyExtraParams = component.verifyExtraParams.text

    }

    override fun getDisplayName(): String = "Paparazzi"
}

private const val GRADLE_COMMAND = "Gradle command"
private const val EXTRA_PARAMS = "Extra params"

class ProjectSettingsComponent {

    private var myMainPanel: JPanel? = null
    val recordCommand = JBTextField()
    val recordExtraParams = JBTextField()

    val verifyCommand = JBTextField()
    val verifyExtraParams = JBTextField()

    fun getPanel(): JPanel? {
        if (myMainPanel == null) {
            val recordPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel(GRADLE_COMMAND), recordCommand, 0, false)
                .addLabeledComponent(JBLabel(EXTRA_PARAMS), recordExtraParams, 0, false)
                .panel
            recordPanel.border = IdeBorderFactory.createTitledBorder("Record Snapshots")
            val verifyPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel(GRADLE_COMMAND), verifyCommand, 0, false)
                .addLabeledComponent(JBLabel(EXTRA_PARAMS), verifyExtraParams, 0, false)
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
