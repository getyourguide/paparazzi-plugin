package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.file
import com.getyourguide.paparazzi.markers.getQualifiedTestName
import com.getyourguide.paparazzi.markers.runGradle
import com.getyourguide.paparazzi.modulePath
import com.getyourguide.paparazzi.service.service
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

private const val ACTION_NAME = "Record Snapshots"

class RecordPaparazziAction(private val psiClass: PsiClass, private val psiMethod: PsiMethod?) :
    AnAction(ACTION_NAME, null, AllIcons.Debugger.Db_set_breakpoint) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val file = psiClass.file()
            if (file != null) {
                val modulePath = project.modulePath(file)
                if (modulePath != null) {
                    val testName = getQualifiedTestName(psiClass, psiMethod)
                    val param = if (testName != null) "--tests $testName" else ""
                    runGradle(
                        project, modulePath, "recordPaparazziDebug $param",
                        RecordTaskCallback(project, psiClass, psiMethod)
                    )
                }
            }
        }
    }
}

class RecordTaskCallback(
    private val project: Project,
    private val psiClass: PsiClass,
    private val psiMethod: PsiMethod?
) : TaskCallback {
    override fun onSuccess() {
        project.service.loadAfterSnapshotsRecorded(psiClass, psiMethod)
    }

    override fun onFailure() {
        // Do nothing.
        // TODO may be notify the user that the operation failed?
    }
}
