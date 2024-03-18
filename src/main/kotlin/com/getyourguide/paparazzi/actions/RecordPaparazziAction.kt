package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.file
import com.getyourguide.paparazzi.getModuleId
import com.getyourguide.paparazzi.getProjectPath
import com.getyourguide.paparazzi.gradleModuleData
import com.getyourguide.paparazzi.markers.getQualifiedTestName
import com.getyourguide.paparazzi.markers.runGradle
import com.getyourguide.paparazzi.service.service
import com.intellij.icons.AllIcons.Debugger.Db_set_breakpoint
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

private const val ACTION_NAME = "Record Snapshots"

/**
 * Runs the Record Paparazzi gradle command with the '--tests' filter enabled for the [test class][psiClass] or for the
 * [test method][psiMethod]
 *
 * @param psiClass the [PsiClass] of the test class for which the snapshots should be recorded
 * @param psiMethod the [PsiMethod] of the test method for which the snapshots should be recorded
 */
class RecordPaparazziAction(private val psiClass: PsiClass, private val psiMethod: PsiMethod?) :
    AnAction(ACTION_NAME, null, Db_set_breakpoint) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = psiClass.file() ?: return
        val gradleData = project.gradleModuleData(file) ?: return
        val path = gradleData.getProjectPath() ?: project.basePath ?: return
        val moduleId = gradleData.getModuleId()
        val testName = getQualifiedTestName(psiClass, psiMethod)
        val task = project.service.settings.recordSnapshotsCommand
        val scriptParams = project.service.settings.recordScriptParams
        val filters = if (testName != null) "--tests $testName" else ""
        val command = if (moduleId != null) {
            "$moduleId:$task $filters"
        } else {
            "$task $filters"
        }.trim()
        runGradle(
            project,
            path,
            command,
            scriptParams,
            RecordTaskCallback(project, psiClass, psiMethod)
        )
    }
}

internal class RecordTaskCallback(
    private val project: Project, private val psiClass: PsiClass, private val psiMethod: PsiMethod?
) : TaskCallback {
    override fun onSuccess() {
        project.service.loadAfterSnapshotsRecorded(psiClass, psiMethod)
    }

    override fun onFailure() {
        // TODO may be notify the user that the operation failed?
    }
}
