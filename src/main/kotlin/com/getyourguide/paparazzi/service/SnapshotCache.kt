package com.getyourguide.paparazzi.service

import com.intellij.openapi.vfs.VirtualFile
import java.awt.Image

// TODO Move the cacheLimit to settings, so that the user can configure this
internal class SnapshotCache(private val cacheLimit: Int = 20) {
    private val cache: MutableList<Snapshot> = mutableListOf()

    operator fun set(file: VirtualFile, image: Image) {
        if (cache.find { it.file == file } == null) {
            if (cache.size >= cacheLimit) {
                cache.removeFirstOrNull()
            }
            cache.add(Snapshot(file, image))
        }
    }

    operator fun get(file: VirtualFile): Image? = cache.find { it.file == file }?.image

    fun clear() = cache.clear()

    private data class Snapshot(val file: VirtualFile, val image: Image)
}
