package com.getyourguide.paparazzi.actions.group

import com.getyourguide.paparazzi.actions.deleteSnapshots
import com.getyourguide.paparazzi.isPaparazziClass
import com.getyourguide.paparazzi.service.FileInfo
import com.getyourguide.paparazzi.service.toFileInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement

private const val ACTION_NAME = "Delete Snapshots"

class GroupDeleteAction : GroupAction(ACTION_NAME, AllIcons.Actions.GC) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val uClass = getPaparazziClass(e)
        if (uClass != null) {
            val file = uClass.javaPsi.containingFile?.virtualFile ?: return
            val fileInfo = file.toFileInfo(project, false)
            val snapshots = fileInfo.allSnapshots()
            val files = snapshots.map { it.file }
            deleteSnapshots(project, files)
        } else {
            val psiDirectory = getPaparazziDirectory(e)
            if (psiDirectory != null) {
                val fileInfoList = getPaparazziFileInfo(psiDirectory, project)
                val snapshots = fileInfoList.flatMap { it.allSnapshots() }.map { it.file }
                deleteSnapshots(project, snapshots)
            }
        }
    }

    private fun getPaparazziFileInfo(psiDirectory: PsiDirectory, project: Project): List<FileInfo> {
        val fileInfoList = mutableListOf<FileInfo>()
        psiDirectory.accept(object : PsiRecursiveElementVisitor() {
            override fun visitFile(file: PsiFile) {
                super.visitFile(file)
                val isPaparazzi = (file.toUElement() as? UFile)?.classes?.find { it.isPaparazziClass() } != null
                if (isPaparazzi) {
                    fileInfoList.add(file.virtualFile.toFileInfo(project, false))
                }
            }
        })
        return fileInfoList
    }
}
