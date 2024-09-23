package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

/**
 * When the toggle is ON, the pre-recorded golden snapshots for the file in the current editor is loaded
 * in the tool window
 */
class AutoLoadFileAction : ToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return project.service.isAutoLoadFileEnabled
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        project.service.isAutoLoadFileEnabled = state
        if (state) project.service.loadFromSelectedEditor(false)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
