package com.getyourguide.paparazzi

import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction
import org.jetbrains.plugins.gradle.service.project.GradleTasksIndices
import java.io.File

internal fun Project.loadFromSelectedEditorFile() {
    val file = FileEditorManager.getInstance(this)?.selectedEditor?.file
    if (file != null) {
        service.reload(file)
    }
}

// TODO add param to run for TestClass, or TestMethod
internal fun Project.runRecordPaparazzi(modulePath: String) {
    // To find the list of tasks. We need to show a UI to select from different build variant
    val tasks = GradleTasksIndices.getInstance(this).findTasks(modulePath, "recordPaparazzi")

    // To execute the gradle command
    GradleExecuteTaskAction.runGradle(this, null, modulePath, "recordPaparazziDebug")
}

internal fun VirtualFile.toItems(project: Project, isFailure: Boolean): List<Item> {
    val psiFile = PsiManager.getInstance(project).findFile(this) as? PsiClassOwner
    return if (psiFile != null) {
        val snapshots = if (isFailure) project.failureDiffSnapshots(this) else project.recordedSnapshots(this)
        psiFile.toItems(snapshots, isFailure)
    } else emptyList()
}

internal fun Project.isToolWindowOpened(): Boolean {
    return ToolWindowManager.getInstance(this).getToolWindow(TOOL_WINDOW_ID)?.isVisible == true
}

private fun PsiClassOwner.toItems(snapshots: List<VirtualFile>, isFailure: Boolean): List<Item> {
    val prefix = if (isFailure) "delta-" else ""
    return classes.flatMap { psiClass ->
        val name = "$prefix${packageName}_${psiClass.name}"
        snapshots.filter { it.name.startsWith(name) }.map {
            Item(it, it.snapshotName(name))
        }
    }
}

private fun VirtualFile.snapshotName(qualifiedTestName: String): String {
    return nameWithoutExtension.substringAfter(qualifiedTestName + "_")
}

private fun Project.recordedSnapshots(file: VirtualFile): List<VirtualFile> {
    return file.getModule(this)?.rootManager?.contentRoots?.find {
        it.name == "test"
    }?.findChild("snapshots")?.findChild("images")?.children?.toList() ?: emptyList()
}

private fun Project.failureDiffSnapshots(file: VirtualFile): List<VirtualFile> {
    val modulePath = modulePath(file)
    val projectPath = basePath
    return if (modulePath != null) {
        LocalFileSystem.getInstance().findFileByPath(modulePath)?.findChild("out")
            ?.findChild("failures")?.children?.toList() ?: emptyList()
    } else if (projectPath != null) {
        LocalFileSystem.getInstance().findFileByPath(projectPath)?.children?.flatMap {
            it.findChild("out")?.findChild("failures")?.children?.toList() ?: emptyList()
        } ?: emptyList()
    } else emptyList()
}

private fun Project.modulePath(file: VirtualFile): String? {
    basePath?.let { projectPath ->
        val relativePath = FileUtil.getRelativePath(projectPath, file.path, File.separatorChar)
        val moduleName = relativePath?.split(File.separator)?.firstOrNull()
        if (moduleName != null) {
            return projectPath + File.separator + moduleName
        }
    }
    return null
}
