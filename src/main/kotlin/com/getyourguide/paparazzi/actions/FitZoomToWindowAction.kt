package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Action to show the snapshots with their width equal to the width of the tool window, maintaining the aspect ratio
 */
class FitZoomToWindowAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service?.zoomFitToWindow()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        e.presentation.isEnabled = !project.service.settings.isFitToWindow
    }
}
