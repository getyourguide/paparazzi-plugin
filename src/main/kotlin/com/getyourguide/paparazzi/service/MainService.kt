package com.getyourguide.paparazzi.service

import com.getyourguide.paparazzi.PaparazziWindowPanel
import com.getyourguide.paparazzi.caretModel
import com.getyourguide.paparazzi.containingMethod
import com.getyourguide.paparazzi.isToolWindowOpened
import com.getyourguide.paparazzi.nonBlocking
import com.getyourguide.paparazzi.psiElement
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
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
    val model: DefaultListModel<Snapshot>
    val settings: Storage

    var onlyShowFailures: Boolean

    fun image(snapshot: Snapshot): Image?

    fun zoomFitToWindow()
    fun zoomActualSize()

    fun loadFromSelectedEditor()
    fun load(file: VirtualFile?, methodName: String? = null)
}

@State(name = "com.getyourguide.paparazzi", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class MainServiceImpl(private val project: Project) : MainService, PersistentStateComponent<MainService.Storage>,
    FileEditorManagerListener {

    private var storage = MainService.Storage()
    private val cache = SoftReference(SnapshotCache())
    private var width: Int = 0

    override var panel: PaparazziWindowPanel? = null
    override val model: DefaultListModel<Snapshot> = DefaultListModel()
    override var onlyShowFailures: Boolean = false

    private var reloadJob: CancellablePromise<List<Snapshot>>? = null

    init {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }

    private var caretListener = CaretModelListener(project)

    override fun selectionChanged(event: FileEditorManagerEvent) {
        super.selectionChanged(event)

        // Remove previously set listeners if any
        event.oldEditor?.caretModel?.removeCaretListener(caretListener)
        event.newEditor?.caretModel?.removeCaretListener(caretListener)

        if (project.isToolWindowOpened()) {
            val file = event.newFile
            if (file != null && (file.extension == "kt" || file.extension == "java")) {
                if (settings.isAutoLoadFileEnabled) {
                    load(file)
                } else if (settings.isAutoLoadMethodEnabled) {
                    val caretModel = event.newEditor.caretModel
                    if (caretModel != null) {
                        nonBlocking {
                            val element = file.psiElement(project, caretModel.offset)
                            val containingUMethod = element?.containingMethod()
                            if (containingUMethod != null) {
                                load(file, containingUMethod.name)
                            } else {
                                load(null)
                            }
                        }
                        caretModel.addCaretListener(caretListener, event.newEditor)
                    }
                }
            }
        }
    }

    private class CaretModelListener(private val project: Project) : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) {
            nonBlocking {
                val file = FileDocumentManager.getInstance().getFile(event.editor.document)
                val offset = event.caret?.offset
                if (file != null && offset != null) {
                    val element = file.psiElement(project, offset)
                    val containingUMethod = element?.containingMethod()
                    if (containingUMethod != null) {
                        project.service.load(file, containingUMethod.name)
                    } else {
                        project.service.load(null)
                    }
                }
            }
        }
    }

    override fun image(snapshot: Snapshot): Image? = cacheImage(snapshot)

    private fun cacheImage(snapshot: Snapshot): Image? {
        val file = snapshot.file
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

    override fun loadFromSelectedEditor() {
        val editor = FileEditorManager.getInstance(project)?.selectedEditor
        val file = editor?.file
        if (settings.isAutoLoadMethodEnabled) {
            val offset = editor?.caretModel?.offset
            if (offset != null) {
                val method = file?.psiElement(project, offset)?.containingMethod()?.name
                if (method != null) load(file, method) else load(null)
            }
        } else {
            if (file != null) load(file)
        }
    }

    private fun reload() {
        val toList = model.elements().toList()
        model.clear()
        toList.forEach { item ->
            model.addElement(item)
        }
        panel?.list?.ensureIndexIsVisible(0)
    }

    override fun load(file: VirtualFile?, methodName: String?) {
        reloadJob?.cancel()
        if (settings.isFitToWindow) {
            width = panel.allowedWidth
        }

        if (file == null) {
            model.clear()
            return
        }
        val nonBlocking: NonBlockingReadAction<List<Snapshot>> = ReadAction.nonBlocking(Callable {
            try {
                model.clear()
                val fileInfo = file.toFileInfo(project, onlyShowFailures)
                val snapshots = if (methodName != null) {
                    fileInfo.snapshotsForMethod(methodName)
                } else {
                    fileInfo.allSnapshots()
                }
                snapshots.map { item ->
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
