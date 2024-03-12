package com.getyourguide.paparazzi.actions.group

import com.getyourguide.paparazzi.isPaparazziClass
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement
import javax.swing.Icon

abstract class GroupAction(name: String, icon: Icon) : AnAction(name, null, icon) {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isVisible = getPaparazziClass(e) != null
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    protected fun getPaparazziClass(e: AnActionEvent): UClass? {
        val file = LangDataKeys.PSI_FILE.getData(e.dataContext)?.toUElement() as? UFile ?: return null
        return file.classes.find { it.isPaparazziClass() }
    }
}
