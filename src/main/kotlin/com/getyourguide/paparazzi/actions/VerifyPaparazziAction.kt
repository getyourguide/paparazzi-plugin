package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.file
import com.getyourguide.paparazzi.markers.getQualifiedTestName
import com.getyourguide.paparazzi.markers.runGradle
import com.getyourguide.paparazzi.testModulePath
import com.getyourguide.paparazzi.nonBlocking
import com.getyourguide.paparazzi.service.service
import com.getyourguide.paparazzi.service.toFileInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

private const val ACTION_NAME = "Verify Snapshots"

/**
 * Runs the Verify Paparazzi gradle command with the '--tests' filter enabled for the [test class][psiClass] or for the
 * [test method][psiMethod]
 *
 * @param psiClass the [PsiClass] of the test class for which the snapshots should be verified
 * @param psiMethod the [PsiMethod] of the test method for which the snapshots should be verified
 */
class VerifyPaparazziAction(private val psiClass: PsiClass, private val psiMethod: PsiMethod?) :
    AnAction(ACTION_NAME, null, AllIcons.Actions.Execute) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = psiClass.file() ?: return
        val modulePath = project.testModulePath(file) ?: return
        val testName = getQualifiedTestName(psiClass, psiMethod)
        val param = if (testName != null) "--tests $testName" else ""
        val gradleCommand = project.service.settings.verifySnapshotsCommand
        val scriptParams = project.service.settings.verifyScriptParams
        runGradle(
            project,
            modulePath,
            "$gradleCommand $param",
            scriptParams,
            VerifyTaskCallback(project, psiClass, psiMethod)
        )
    }
}

internal class VerifyTaskCallback(
    private val project: Project, private val psiClass: PsiClass, private val psiMethod: PsiMethod?
) : TaskCallback {

    override fun onSuccess() {
        project.service.loadAfterSnapshotsRecorded(psiClass, psiMethod)
        deleteFailureSnapshots(psiClass, psiMethod)
    }

    override fun onFailure() {
        project.service.loadAfterSnapshotsVerified(psiClass, psiMethod)
    }

    private fun deleteFailureSnapshots(psiClass: PsiClass, psiMethod: PsiMethod?) {
        nonBlocking(asyncAction = {
            val file = psiClass.containingFile?.virtualFile
            if (file != null) {
                val fileInfo = file.toFileInfo(project, true)
                if (psiMethod != null) {
                    fileInfo.snapshotsForMethod(psiMethod.name)
                } else {
                    fileInfo.allSnapshots()
                }
            } else emptyList()
        }) { snapshots ->
            try {
                snapshots.forEach {
                    WriteAction.run<Throwable> {
                        it.file.delete(this)
                    }
                }
            } catch (t: Throwable) {
                // TODO Notify user why the file couldn't be deleted, perhaps via notifications or in the log
            }
        }
    }
}
