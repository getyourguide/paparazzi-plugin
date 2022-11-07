package com.getyourguide.paparazzi.service

import com.getyourguide.paparazzi.Item
import com.getyourguide.paparazzi.PaparazziWindowPanel
import com.getyourguide.paparazzi.isToolWindowOpened
import com.getyourguide.paparazzi.toItems
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.CancellablePromise
import java.awt.Image
import java.lang.ref.SoftReference
import java.util.concurrent.Callable
import javax.imageio.ImageIO
import javax.swing.DefaultListModel

const val HORIZONTAL_PADDING = 16

interface MainService {

    class Storage {
        // path to snapshots
        // no of snapshots to show at a time (everything at once can cause OOM)
        var isAutoLoadFileEnabled = true
            set(value) {
                field = value
                if (value) isAutoLoadMethodEnabled = false
            }
        var isAutoLoadMethodEnabled = false
            set(value) {
                field = value
                if (value) isAutoLoadFileEnabled = false
            }
        var isFitToWindow = true
    }

    var panel: PaparazziWindowPanel?
    val model: DefaultListModel<Item>
    val settings: Storage

    var onlyShowFailures: Boolean

    fun image(item: Item): Image?

    fun zoomFitToWindow()
    fun zoomActualSize()

    fun reload(file: VirtualFile)
}

@State(name = "com.getyourguide.paparazzi", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class MainServiceImpl(private val project: Project) : MainService, PersistentStateComponent<MainService.Storage>,
    FileEditorManagerListener {

    private var storage = MainService.Storage()
    private val cache = SoftReference(SnapshotCache())
    private var width: Int = 0

    override var panel: PaparazziWindowPanel? = null
    override val model: DefaultListModel<Item> = DefaultListModel()
    override var onlyShowFailures: Boolean = false

    private var reloadJob: CancellablePromise<List<Item>>? = null

    init {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        super.selectionChanged(event)
        if (project.isToolWindowOpened()) {
            val file = event.newFile
            if (file != null && (file.extension == "kt" || file.extension == "java")) {
                if (settings.isAutoLoadFileEnabled) {
                    reload(file)
                } else if (settings.isAutoLoadMethodEnabled) {
                    val caret = (event.newEditor as? TextEditor)?.editor?.caretModel
                    if (caret != null) {
                        val offset = caret.offset
                    }
                }
            }
        }
    }

    override fun image(item: Item): Image? = cacheImage(item)

    private fun cacheImage(item: Item): Image? {
        val file = item.file
        return cache.get()?.get(file) ?: try {
            file.inputStream.use {
                val bufferedImage = ImageIO.read(it)
                val finalImage = if (width == 0) {
                    bufferedImage
                } else {
                    val width = bufferedImage.width.toFloat()
                    val height = bufferedImage.height.toFloat()
                    val newWidth = this.width
                    var newHeight = (height / width * newWidth).toInt()
                    if (newHeight == 0) newHeight = 20
                    bufferedImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
                }
                cache.get()?.set(file, finalImage)
                finalImage
            }
        } catch (e: Exception) {
            // Log the exception to the notification channel
            null
        }
    }

    override fun zoomFitToWindow() {
        panel?.let {
            settings.isFitToWindow = true
            width = it.allowedWidth
            reload()
        }
    }

    override fun zoomActualSize() {
        settings.isFitToWindow = false
        width = 0
        reload()
    }

    private fun reload() {
        val toList = model.elements().toList()
        model.clear()
        toList.forEach { item ->
            model.addElement(item)
        }
        panel?.list?.ensureIndexIsVisible(0)
    }

    override fun reload(file: VirtualFile) {
        reloadJob?.cancel()
        if (settings.isFitToWindow) {
            width = panel.allowedWidth
        }

        val nonBlocking: NonBlockingReadAction<List<Item>> = ReadAction.nonBlocking(Callable {
            try {
                model.clear()
                file.toItems(project, onlyShowFailures).map { item ->
                    ProgressManager.checkCanceled()
                    cacheImage(item)
                    item
                }
            } catch (e: ProcessCanceledException) {
                emptyList()
            }
        })
        reloadJob = nonBlocking.finishOnUiThread(ModalityState.defaultModalityState()) {
            model.clear()
            model.addAll(it)
            panel?.list?.ensureIndexIsVisible(0)
        }.submit(AppExecutorUtil.getAppExecutorService())
    }

    override fun getState(): MainService.Storage = storage

    override fun loadState(state: MainService.Storage) {
        storage = state
    }

    override val settings: MainService.Storage
        get() = state

    private val PaparazziWindowPanel?.allowedWidth: Int
        get() {
            return if (this != null) {
                (this.width - HORIZONTAL_PADDING * 2).coerceIn(200, 500)
            } else 0
        }
}

val Project.service: MainService
    get() {
        return this.getService(MainService::class.java)
    }
