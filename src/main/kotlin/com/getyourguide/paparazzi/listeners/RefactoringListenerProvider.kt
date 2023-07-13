package com.getyourguide.paparazzi.listeners

import com.getyourguide.paparazzi.getUClass
import com.getyourguide.paparazzi.isPaparazziTestClass
import com.getyourguide.paparazzi.isTestClass
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementAdapter
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider

class RefactoringListenerProvider : RefactoringElementListenerProvider {

    override fun getListener(element: PsiElement?): RefactoringElementListener? {
        println("Element refactored method listener")
        if (element == null) return null
        if (element.isPaparazziTestClass()) {
            val psiClass = element.getUClass()?.javaPsi ?: return null
            if (psiClass.isTestClass()) {
                println("Element refactored class = $psiClass")
                return ElementAdapter(psiClass.name ?: "EMPTY")
            }
        }
        return null
    }
}

private class ElementAdapter(val oldName: String) : RefactoringElementAdapter() {
    override fun undoElementMovedOrRenamed(newElement: PsiElement, oldQualifiedName: String) {
        println("Element oldName = $oldQualifiedName, newName = ${newElement.toString()}, item = $oldName")
    }

    override fun elementRenamedOrMoved(newElement: PsiElement) {
        println("Element newName = ${newElement.toString()}, item = $oldName")
    }
}
