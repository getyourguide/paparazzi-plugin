package com.getyourguide.paparazzi.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.getyourguide.paparazzi.service.service

class ActualSizeAction : AnAction() {

    companion object {
        const val ID = "com.getyourguide.paparazzi.actions.ActualSizeAction"
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service?.zoomActualSize()
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        if (project != null) {
            e.presentation.isEnabled = project.service.settings.isFitToWindow
        }
    }
}
