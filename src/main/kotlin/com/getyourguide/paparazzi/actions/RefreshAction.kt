package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Action to force reload the snapshots based on the filters applied in the tool window
 */
class RefreshAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service?.loadFromSelectedEditor(true)
    }
}
