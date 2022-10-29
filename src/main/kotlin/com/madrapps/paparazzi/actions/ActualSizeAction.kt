package com.madrapps.paparazzi.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.madrapps.paparazzi.service.service

class ActualSizeAction : AnAction() {

    companion object {
        const val ID = "com.madrapps.paparazzi.actions.ActualSizeAction"
    }

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service?.zoomActualSize()
    }
}
