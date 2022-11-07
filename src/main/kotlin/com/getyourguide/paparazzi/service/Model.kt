package com.getyourguide.paparazzi.service

import com.getyourguide.paparazzi.methods
import com.getyourguide.paparazzi.toSnapshots
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

data class Snapshot(val file: VirtualFile, val name: String)

data class MethodInfo(val name: String, val snapshots: List<Snapshot>)

data class FileInfo(val items: List<MethodInfo>) {

    fun snapshotsForMethod(name: String): List<Snapshot> {
        return items.find { it.name == name }?.snapshots ?: emptyList()
    }

    fun allSnapshots(): List<Snapshot> = items.flatMap { it.snapshots }
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
