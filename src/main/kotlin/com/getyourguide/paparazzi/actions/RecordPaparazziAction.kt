package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.file
import com.getyourguide.paparazzi.modulePath
import com.getyourguide.paparazzi.service.service
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.gradle.util.GradleConstants

class RecordPaparazziAction(name: String, private val psiClass: PsiClass, private val psiMethod: PsiMethod?) :
    AnAction(name, null, AllIcons.Debugger.Db_set_breakpoint) {

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

    private fun getQualifiedTestName(psiClass: PsiClass, psiMethod: PsiMethod?): String? {
        val className = psiClass.qualifiedName
        if (className != null) {
            val methodName = psiMethod?.name
            return if (methodName != null) "$className.$methodName" else className
        }
        return null
    }

    private fun runGradle(project: Project, path: String, fullCommandLine: String, callback: RecordTaskCallback) {
        val settings = ExternalSystemTaskExecutionSettings()
        settings.externalProjectPath = path
        settings.taskNames = fullCommandLine.trim().split(" ")
        settings.externalSystemIdString = GradleConstants.SYSTEM_ID.toString()

        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID,
            callback,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            false
        )
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
        // TODO may be notify user that the operation failed?
    }
}
