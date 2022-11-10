package com.getyourguide.paparazzi.markers

import com.getyourguide.paparazzi.actions.RecordPaparazziAction
import com.getyourguide.paparazzi.actions.VerifyPaparazziAction
import com.intellij.codeInsight.TestFrameworks
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
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.toUElement

private const val PAPARAZZI_IMPORT = "app.cash.paparazzi.Paparazzi"

class PaparazziRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element.isIdentifier && element.isPaparazziTestClass()) {
            val (psiClass, psiMethod) = getTestMethod(element)
            if (psiClass != null && psiMethod != null) {
                return Info(
                    AllIcons.RunConfigurations.TestState.Run,
                    arrayOf(RecordPaparazziAction(psiClass, psiMethod), VerifyPaparazziAction(psiClass, psiMethod)),
                    null
                )
            }
            val testClass = getTestClass(element)
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

    private fun getTestMethod(element: PsiElement): Pair<PsiClass?, PsiMethod?> {
        val psiMethod = (element.parent.toUElement() as? UMethod)?.javaPsi
        if (psiMethod != null) {
            val psiClass = element.containingUClass()?.javaPsi
            if (psiClass != null) {
                val framework = TestFrameworks.detectFramework(psiClass)
                if (framework?.isTestMethod(psiMethod) == true) {
                    return psiClass to psiMethod
                }
            }
        }
        return null to null
    }

    private fun getTestClass(element: PsiElement): PsiClass? {
        val psiClass = (element.parent.toUElement() as? UClass)?.javaPsi
        if (psiClass != null) {
            val framework = TestFrameworks.detectFramework(psiClass)
            if (framework?.isTestClass(psiClass) == true) {
                return psiClass
            }
        }
        return null
    }

    private val PsiElement.isIdentifier
        get() = toUElement() is UIdentifier

    private fun PsiElement.isPaparazziTestClass(): Boolean {
        val uClass = containingUClass()
        if (uClass != null) {
            if (uClass.hasImport(PAPARAZZI_IMPORT)) {
                return true
            } else {
                uClass.javaPsi.supers.forEach { psiClass ->
                    if ((psiClass.toUElement() as? UClass)?.hasImport(PAPARAZZI_IMPORT) == true) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun UClass.hasImport(packageName: String): Boolean {
        val uFile = getContainingUFile()
        return uFile?.imports?.find { it.asSourceString().contains(packageName) } != null
    }

    private fun PsiElement.containingUClass(): UClass? {
        return (parents.toList().map { it.toUElement() }.find { it is UClass } as? UClass)
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

internal fun runGradle(project: Project, path: String, fullCommandLine: String, scriptParams: String, callback: TaskCallback) {
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