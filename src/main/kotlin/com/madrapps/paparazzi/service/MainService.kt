package com.madrapps.paparazzi.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.madrapps.paparazzi.Item
import com.madrapps.paparazzi.PaparazziWindowPanel
import java.awt.Image
import javax.imageio.ImageIO
import javax.swing.DefaultListModel

interface MainService {

    class Storage {
        // path to snapshots
        // no of screenshots to show at a time (everything at once can cause OOM)
    }

    var panel: PaparazziWindowPanel?
    val model: DefaultListModel<Item>

    fun image(item: Item): Image

    fun zoomFitToWindow()
    fun zoomActualSize()

    fun reload()
}

@State(name = "com.madrapps.paparazzi", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class MainServiceImpl(private val project: Project) : MainService, PersistentStateComponent<MainService.Storage>,
    FileEditorManagerListener {

    private val MAX_ZOOM_WIDTH = 700
    private val MIN_ZOOM_WIDTH = 200

    private var storage = MainService.Storage()
    private val screenshotMap: MutableMap<VirtualFile, Image> = mutableMapOf()
    private var width: Int = 0

    override var panel: PaparazziWindowPanel? = null
    override val model: DefaultListModel<Item> = DefaultListModel()

    init {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        super.selectionChanged(event)
        println("File being viewed - ${event.newFile.nameWithoutExtension}")
    }

    override fun image(item: Item): Image {
        val file = item.file
        val image = screenshotMap[file]
        return if (image == null) {
            val read = ImageIO.read(file.inputStream)
            val im = if (width == 0) {
                read
            } else {
                val width = read.width.toFloat()
                val height = read.height.toFloat()
                val newWidth = this.width
                var newHeight = (height / width * newWidth).toInt()
                if (newHeight == 0) newHeight = 20
                read.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
            }
            screenshotMap[file] = im
            im
        } else {
            image
        }
    }


    override fun zoomFitToWindow() {
        screenshotMap.clear()
        panel?.let {
            val tmp = it.width - 32
            width = if (tmp > MAX_ZOOM_WIDTH) {
                MAX_ZOOM_WIDTH
            } else if (tmp < MIN_ZOOM_WIDTH) {
                MIN_ZOOM_WIDTH
            } else {
                tmp
            }
            reload()
        }
    }

    override fun zoomActualSize() {
        screenshotMap.clear()
        width = 0
        reload()
    }

    override fun reload() {
        val toList = model.elements().toList()
        model.clear()
        toList.forEach { item ->
            model.addElement(item)
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
