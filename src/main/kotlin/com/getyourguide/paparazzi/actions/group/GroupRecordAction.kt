package com.getyourguide.paparazzi.actions.group

import com.getyourguide.paparazzi.actions.RecordTaskCallback
import com.getyourguide.paparazzi.file
import com.getyourguide.paparazzi.markers.getQualifiedTestName
import com.getyourguide.paparazzi.markers.runGradle
import com.getyourguide.paparazzi.modulePath
import com.getyourguide.paparazzi.service.service
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.getPackage

private const val ACTION_NAME = "Record Snapshots"

class GroupRecordAction : GroupAction(ACTION_NAME, AllIcons.Debugger.Db_set_breakpoint) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val uClass = getPaparazziClass(e)
        if (uClass != null) {
            val file = uClass.file() ?: return
            val modulePath = project.modulePath(file) ?: return
            val psiClass = uClass.javaPsi
            val testName = getQualifiedTestName(psiClass, null)
            runRecordTask(project, testName, modulePath, RecordTaskCallback(project, psiClass, null))
        } else {
            val psiDirectory = getPaparazziDirectory(e)
            if (psiDirectory != null) {
                val modulePath = project.modulePath(psiDirectory.virtualFile) ?: return
                val psiPackage = psiDirectory.getPackage() ?: return
                val testName = psiPackage.qualifiedName + ".*"
                runRecordTask(project, testName, modulePath)
            }
        }
    }

    private fun runRecordTask(
        project: Project,
        testName: String?,
        modulePath: String,
        callback: RecordTaskCallback? = null,
    ) {
        val gradleCommand = project.service.settings.recordSnapshotsCommand
        val scriptParams = project.service.settings.recordScriptParams
        val filters = if (testName != null) "--tests $testName" else ""
        runGradle(
            project,
            modulePath,
            "$gradleCommand $filters",
            scriptParams,
            callback
        )
    }
}
