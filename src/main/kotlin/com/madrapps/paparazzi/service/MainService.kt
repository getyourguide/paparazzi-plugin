package com.madrapps.paparazzi.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.madrapps.paparazzi.Item
import com.madrapps.paparazzi.PaparazziWindowPanel
import org.jetbrains.kotlin.idea.util.application.getService
import java.awt.Image
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JLabel

interface MainService {

    class Storage {
        // path to snapshots
        // no of screenshots to show at a time (everything at once can cause OOM)
    }

    var panel: PaparazziWindowPanel?

    fun image(item: Item): Image
}

@State(name = "com.madrapps.paparazzi", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class MainServiceImpl(private val project: Project) : MainService, PersistentStateComponent<MainService.Storage> {

    private var storage = MainService.Storage()
    private val screenshotMap: MutableMap<VirtualFile, Image> = mutableMapOf()

    override var panel: PaparazziWindowPanel? = null

    override fun image(item: Item): Image {
        val file = item.file
        val image = screenshotMap[file]
        return if (image == null) {
            val read = ImageIO.read(file.inputStream)
            val width = read.width.toFloat()
            val height = read.height.toFloat()
            val newHeight = (height / width * 300).toInt()
            //val scaledInstance = read.getScaledInstance(300, newHeight, Image.SCALE_SMOOTH)
            screenshotMap[file] = read
            read
        } else {
            image
        }
    }


    override fun getState(): MainService.Storage {
        return storage
    }

    override fun loadState(state: MainService.Storage) {
        storage = state
    }

}

val Project.service: MainService
    get() {
        return this.getService(MainService::class.java)
    }