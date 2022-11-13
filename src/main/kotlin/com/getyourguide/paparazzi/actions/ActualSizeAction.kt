package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Action to show the snapshots in their actual size
 */
class ActualSizeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service?.zoomActualSize()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        e.presentation.isEnabled = project.service.settings.isFitToWindow
    }
}
