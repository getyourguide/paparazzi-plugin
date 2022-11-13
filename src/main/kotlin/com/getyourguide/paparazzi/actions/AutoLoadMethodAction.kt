package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

/**
 * When the toggle is ON, the pre-recorded golden snapshots for the current method (based on the caret position) in
 * the editor is loaded in the tool window
 */
class AutoLoadMethodAction : ToggleAction() {

    override fun isSelected(e: AnActionEvent): Boolean {
        val project = e.project ?: return false
        return project.service.isAutoLoadMethodEnabled
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val project = e.project ?: return
        project.service.isAutoLoadMethodEnabled = state
        if (state) project.service.loadFromSelectedEditor(false)
    }
}
