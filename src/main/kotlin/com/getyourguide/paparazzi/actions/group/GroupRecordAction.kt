package com.getyourguide.paparazzi.actions.group

import com.getyourguide.paparazzi.actions.RecordTaskCallback
import com.getyourguide.paparazzi.file
import com.getyourguide.paparazzi.getProjectPath
import com.getyourguide.paparazzi.gradleModuleData
import com.getyourguide.paparazzi.markers.getQualifiedTestName
import com.getyourguide.paparazzi.markers.runGradle
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
            val gradleData = project.gradleModuleData(file) ?: return
            val projectPath = gradleData.getProjectPath() ?: project.basePath ?: return
            val psiClass = uClass.javaPsi
            val testName = getQualifiedTestName(psiClass, null)
            runRecordTask(
                project = project,
                moduleId = gradleData.data.id,
                testName = testName,
                projectPath = projectPath,
                callback = RecordTaskCallback(project, psiClass, null)
            )
            return
        }
        val psiDirectory = getPaparazziDirectory(e)
        if (psiDirectory != null) {
            val gradleData = gradleModuleData(psiDirectory) ?: return
            val projectPath = gradleData.getProjectPath() ?: project.basePath ?: return
            val psiPackage = psiDirectory.getPackage() ?: return
            val testName = psiPackage.qualifiedName + ".*"
            runRecordTask(
                project = project,
                moduleId = gradleData.data.id,
                testName = testName,
                projectPath = projectPath,
            )
            return
        }
        val module = getPaparazziModule(e)
        if (module != null) {
            val gradleData = gradleModuleData(module) ?: return
            val projectPath = gradleData.getProjectPath() ?: project.basePath ?: return
            runRecordTask(
                project = project,
                moduleId = gradleData.data.id,
                testName = null,
                projectPath = projectPath,
            )
            return
        }
    }

    private fun runRecordTask(
        project: Project,
        moduleId: String,
        testName: String?,
        projectPath: String,
        callback: RecordTaskCallback? = null,
    ) {
        val gradleCommand = project.service.settings.recordSnapshotsCommand
        val scriptParams = project.service.settings.recordScriptParams
        val filters = if (testName != null) "--tests $testName" else ""
        runGradle(
            project,
            projectPath,
            "$moduleId:$gradleCommand $filters",
            scriptParams,
            callback
        )
    }
}
