package com.getyourguide.paparazzi.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.getyourguide.paparazzi.service.service


class RefreshAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val file = FileEditorManager.getInstance(project)?.selectedEditor?.file
            if (file != null) {
                e.project?.service?.reload(file)
            }
        }
    }
}
