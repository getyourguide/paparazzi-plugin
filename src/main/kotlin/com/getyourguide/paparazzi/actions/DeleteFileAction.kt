package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.service.service
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VirtualFile

private const val ACTION_NAME = "Delete snapshot"

/**
 * Action to delete the snapshot file
 */
class DeleteFileAction(val file: VirtualFile) : AnAction(ACTION_NAME, null, AllIcons.Actions.GC) {

    override fun actionPerformed(e: AnActionEvent) {
        WriteAction.run<Throwable> {
            file.delete(this)
            e.project?.service?.loadFromSelectedEditor(true)
        }
    }
}
