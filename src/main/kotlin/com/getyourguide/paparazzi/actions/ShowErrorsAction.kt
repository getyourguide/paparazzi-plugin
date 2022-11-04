package com.getyourguide.paparazzi.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.getyourguide.paparazzi.service.service


class ShowErrorsAction : ToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return project.service.onlyShowFailures
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project
        if (project != null) {
            project.service.onlyShowFailures = state
            val file = FileEditorManager.getInstance(project)?.selectedEditor?.file
            if (file != null) {
                e.project?.service?.reload(file)
            }
        }
    }
}
