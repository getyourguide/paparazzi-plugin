package com.getyourguide.paparazzi.actions.group

import com.getyourguide.paparazzi.actions.deleteSnapshots
import com.getyourguide.paparazzi.service.toFileInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

private const val ACTION_NAME = "Delete Snapshots"

class GroupDeleteAction : GroupAction(ACTION_NAME, AllIcons.Actions.GC) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiClass = getPaparazziClass(e)?.javaPsi ?: return
        val file = psiClass.containingFile?.virtualFile ?: return
        val fileInfo = file.toFileInfo(project, false)
        val snapshots = fileInfo.allSnapshots()
        val files = snapshots.map { it.file }
        deleteSnapshots(project, files)
    }
}
