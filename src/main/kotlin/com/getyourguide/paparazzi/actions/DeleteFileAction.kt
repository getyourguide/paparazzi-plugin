package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.service.service
import com.getyourguide.paparazzi.service.toFileInfo
import com.intellij.icons.AllIcons
import com.intellij.ide.util.DeleteHandler
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.core.util.toPsiFile

/**
 * Action to delete the snapshot file
 */
class DeleteFileAction(
    private val files: List<VirtualFile> = emptyList(),
    private val psiClass: PsiClass? = null,
    private val psiMethod: PsiMethod? = null
) : AnAction(getActionName(files), null, AllIcons.Actions.GC) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (psiClass != null) {
            val file = psiClass.containingFile?.virtualFile ?: return
            val fileInfo = file.toFileInfo(project, false)
            val methodName = psiMethod?.name
            val snapshots = if (methodName != null) {
                fileInfo.snapshotsForMethod(methodName)
            } else {
                fileInfo.allSnapshots()
            }
            val files = snapshots.map { it.file }
            deleteSnapshots(project, files)
        } else {
            deleteSnapshots(project, files)
        }
    }

    private fun deleteSnapshots(project: Project, files: List<VirtualFile>) {
        val psiFiles = files.mapNotNull { it.toPsiFile(project) }
        DeleteHandler.deletePsiElement(psiFiles.toTypedArray(), project, true)
        project.service.loadFromSelectedEditor(true)
    }
}

private fun getActionName(files: List<VirtualFile>): String {
    return if (files.size == 1) "Delete Snapshot" else "Delete All Snapshots"
}
