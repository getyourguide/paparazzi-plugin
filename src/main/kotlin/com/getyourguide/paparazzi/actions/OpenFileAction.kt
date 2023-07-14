package com.getyourguide.paparazzi.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile

private const val ACTION_NAME = "Open in Editor"

/**
 * Action to open the snapshot file in the editor window
 */
class OpenFileAction(val file: VirtualFile) : AnAction(ACTION_NAME) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val fileManager = FileEditorManager.getInstance(project)
            fileManager.openFile(file, false)
        }
    }
}
