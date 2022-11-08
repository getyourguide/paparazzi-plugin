package com.getyourguide.paparazzi.actions

import com.getyourguide.paparazzi.file
import com.getyourguide.paparazzi.modulePath
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction

class RecordPaparazziAction(name: String, private val psiClass: PsiClass, private val psiMethod: PsiMethod?) :
    AnAction(name, null, AllIcons.Debugger.Db_set_breakpoint) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val file = psiClass.file(project)
            if (file != null) {
                val modulePath = project.modulePath(file)
                if (modulePath != null) {
                    val testName = getQualifiedTestName(psiClass, psiMethod)
                    val param = if (testName != null) "--tests $testName" else ""
                    GradleExecuteTaskAction.runGradle(
                        project,
                        null,
                        modulePath,
                        "recordPaparazziDebug $param"
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
}