package com.getyourguide.paparazzi.actions.group

import com.getyourguide.paparazzi.isPaparazziClass
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import javax.swing.Icon

abstract class GroupAction(name: String, icon: Icon) : AnAction(name, null, icon) {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isVisible =
            getPaparazziClass(e) != null || getPaparazziDirectory(e) != null || getPaparazziModule(e) != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    protected fun getPaparazziClass(e: AnActionEvent): UClass? {
        val file = LangDataKeys.PSI_FILE.getData(e.dataContext)?.toUElement() as? UFile ?: return null
        return file.classes.find { it.isPaparazziClass() }
    }

    protected fun getPaparazziDirectory(e: AnActionEvent): PsiDirectory? {
        val psiDirectory = LangDataKeys.IDE_VIEW.getData(e.dataContext)?.directories?.firstOrNull() ?: return null
        if (!psiDirectory.isPackage()) return null
        var found = false
        psiDirectory.accept(object : PsiRecursiveElementVisitor() {
            override fun visitFile(file: PsiFile) {
                super.visitFile(file)
                found = (file.toUElement() as? UFile)?.classes?.find { it.isPaparazziClass() } != null
                if (found) return
            }
        })
        return if (found) psiDirectory else null
    }

    protected fun getPaparazziModule(e: AnActionEvent): PsiDirectory? {
        val psiDirectory = LangDataKeys.IDE_VIEW.getData(e.dataContext)?.directories?.firstOrNull() ?: return null
        ModuleUtilCore.findModuleForPsiElement(psiDirectory) ?: return null
        return psiDirectory
    }

    private fun PsiDirectory.isPackage(): Boolean {
        return getPackage()?.qualifiedName?.isNotEmpty() == true
    }
}
