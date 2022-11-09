package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.file
import com.getyourguide.paparazzi.markers.getQualifiedTestName
import com.getyourguide.paparazzi.markers.runGradle
import com.getyourguide.paparazzi.modulePath
import com.getyourguide.paparazzi.service.service
import com.getyourguide.paparazzi.service.toFileInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable

private const val ACTION_NAME = "Verify Snapshots"

class VerifyPaparazziAction(private val psiClass: PsiClass, private val psiMethod: PsiMethod?) :
    AnAction(ACTION_NAME, null, AllIcons.Actions.Execute) {

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
                        project, modulePath, "verifyPaparazziDebug $param",
                        VerifyTaskCallback(project, psiClass, psiMethod)
                    )
                }
            }
        }
    }
}

class VerifyTaskCallback(
    private val project: Project,
    private val psiClass: PsiClass,
    private val psiMethod: PsiMethod?
) : TaskCallback {
    override fun onSuccess() {
        project.service.loadAfterSnapshotsRecorded(psiClass, psiMethod)
        deleteFailureSnapshots(psiClass, psiMethod)
    }

    override fun onFailure() {
        project.service.loadAfterSnapshotsVerified(psiClass, psiMethod)
    }

    private fun deleteFailureSnapshots(psiClass: PsiClass, psiMethod: PsiMethod?) {
        ReadAction.nonBlocking(Callable {
            val file = psiClass.containingFile?.virtualFile
            if (file != null) {
                val fileInfo = file.toFileInfo(project, true)
                if (psiMethod != null) {
                    fileInfo.snapshotsForMethod(psiMethod.name)
                } else {
                    fileInfo.allSnapshots()
                }
            } else emptyList()
        }).finishOnUiThread(ModalityState.defaultModalityState()) { snapshots ->
            try {
                snapshots.forEach {
                    WriteAction.run<Throwable> {
                        it.file.delete(this)
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }
}
