package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class RefreshAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service?.loadFromSelectedEditor()
    }
}
