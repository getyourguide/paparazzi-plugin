package com.getyourguide.paparazzi.service

import com.getyourguide.paparazzi.containingMethod
import com.getyourguide.paparazzi.methods
import com.getyourguide.paparazzi.psiElement
import com.getyourguide.paparazzi.toSnapshots
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable

data class Snapshot(val file: VirtualFile, val name: String)

data class MethodInfo(val name: String, val snapshots: List<Snapshot>)

data class FileInfo(val items: List<MethodInfo>) {

    fun snapshotsForMethod(name: String): List<Snapshot> {
        return items.find { it.name == name }?.snapshots ?: emptyList()
    }

    fun allSnapshots(): List<Snapshot> = items.flatMap { it.snapshots }
}

data class PreviouslyLoaded(val file: VirtualFile? = null, val methodName: String? = null) {
    fun check(newFile: VirtualFile?, newMethodName: String?): Boolean {
        if (newFile != null && newMethodName != null) {
            return newFile.name == file?.name && newMethodName == methodName
        }
        return false
    }
}

internal fun VirtualFile.toFileInfo(project: Project, isFailure: Boolean): FileInfo {
    val snapshots = toSnapshots(project, isFailure).toMutableList()
    val methods = methods(project).sortedByDescending { it.length }
    return FileInfo(methods.map { name ->
        val filtered = snapshots.filter { it.name.startsWith(name) }
        snapshots.removeAll(filtered)
        MethodInfo(name, filtered)
    })
}

class CaretModelListener(private val project: Project) : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
        val file = FileDocumentManager.getInstance().getFile(event.editor.document)
        val offset = event.caret?.offset
        if (file != null && offset != null) {
            load(file, offset)
        }
    }

    fun load(file: VirtualFile, offset: Int) {
        ReadAction.nonBlocking(Callable {
            try {
                val psiElement = file.psiElement(project, offset)
                val method = psiElement?.containingMethod()?.name
                Pair(file, method)
            } catch (e: IndexNotReadyException) {
                Pair(null, null)
            }
        }).finishOnUiThread(ModalityState.defaultModalityState()) { (file, method) ->
            with(project.service) {
                if (method != null) load(file, method) else load(null)
            }
        }.submit(AppExecutorUtil.getAppExecutorService())
    }
}
