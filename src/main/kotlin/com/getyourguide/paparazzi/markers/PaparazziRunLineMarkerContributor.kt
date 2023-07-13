package com.getyourguide.paparazzi.markers

import com.getyourguide.paparazzi.actions.RecordPaparazziAction
import com.getyourguide.paparazzi.actions.VerifyPaparazziAction
import com.getyourguide.paparazzi.getTestClass
import com.getyourguide.paparazzi.getTestMethod
import com.getyourguide.paparazzi.isIdentifier
import com.getyourguide.paparazzi.isPaparazziTestClass
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.gradle.util.GradleConstants

class PaparazziRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element.isIdentifier && element.isPaparazziTestClass()) {
            val (psiClass, psiMethod) = element.getTestMethod()
            if (psiClass != null && psiMethod != null) {
                return Info(
                    AllIcons.RunConfigurations.TestState.Run,
                    arrayOf(RecordPaparazziAction(psiClass, psiMethod), VerifyPaparazziAction(psiClass, psiMethod)),
                    null
                )
            }
            val testClass = element.getTestClass()
            if (testClass != null) {
                return Info(
                    AllIcons.RunConfigurations.TestState.Run,
                    arrayOf(RecordPaparazziAction(testClass, null), VerifyPaparazziAction(testClass, null)),
                    null
                )
            }
        }
        return null
    }
}

internal fun getQualifiedTestName(psiClass: PsiClass, psiMethod: PsiMethod?): String? {
    val className = psiClass.qualifiedName
    if (className != null) {
        val methodName = psiMethod?.name
        return if (methodName != null) "$className.$methodName" else className
    }
    return null
}

internal fun runGradle(
    project: Project,
    path: String,
    fullCommandLine: String,
    scriptParams: String,
    callback: TaskCallback
) {
    val settings = ExternalSystemTaskExecutionSettings()
    settings.externalProjectPath = path
    settings.taskNames = fullCommandLine.trim().split(" ")
    settings.scriptParameters = scriptParams
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
