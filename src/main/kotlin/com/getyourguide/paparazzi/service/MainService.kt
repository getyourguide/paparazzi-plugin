package com.getyourguide.paparazzi.service

import com.getyourguide.paparazzi.Item
import com.getyourguide.paparazzi.PaparazziWindowPanel
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import java.awt.Image
import javax.imageio.ImageIO
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities

const val HORIZONTAL_PADDING = 16

interface MainService {

    class Storage {
        // path to snapshots
        // no of screenshots to show at a time (everything at once can cause OOM)
        var isAutoChangeEnabled = true
        var isFitToWindow = true
    }

    var panel: PaparazziWindowPanel?
    val model: DefaultListModel<Item>
    val settings: Storage

    var onlyShowFailures: Boolean

    fun image(item: Item): Image

    fun zoomFitToWindow()
    fun zoomActualSize()

    fun reload()
    fun reload(file: VirtualFile)
}

@State(name = "com.getyourguide.paparazzi", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class MainServiceImpl(private val project: Project) : MainService, PersistentStateComponent<MainService.Storage>,
    FileEditorManagerListener {

    private var storage = MainService.Storage()
    private val snapshotsMap: MutableMap<VirtualFile, Image> = mutableMapOf()
    private var width: Int = 0

    override var panel: PaparazziWindowPanel? = null
    override val model: DefaultListModel<Item> = DefaultListModel()
    override var onlyShowFailures: Boolean = false

    init {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        super.selectionChanged(event)
        if (settings.isAutoChangeEnabled) {
            event.newFile?.let {
                if (it.extension == "kt" || it.extension == "java") {
                    reload(it)
                }
            }
        }
    }

    override fun image(item: Item): Image {
        val file = item.file
        val image = snapshotsMap[file]
        return if (image != null) {
            image
        } else {
            val bufferedImage = ImageIO.read(file.inputStream)
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
            snapshotsMap[file] = finalImage
            finalImage
        }
    }

    override fun zoomFitToWindow() {
        snapshotsMap.clear()
        panel?.let {
            settings.isFitToWindow = true
            width = it.allowedWidth
            reload()
        }
    }

    override fun zoomActualSize() {
        snapshotsMap.clear()
        settings.isFitToWindow = false
        width = 0
        reload()
    }

    override fun reload() {
        snapshotsMap.clear() // FIXME enable LRU cache
        val toList = model.elements().toList()
        model.clear()
        toList.forEach { item ->
            model.addElement(item)
        }
        panel?.list?.ensureIndexIsVisible(0)
    }

    override fun reload(file: VirtualFile) {
        SwingUtilities.invokeLater {
            snapshotsMap.clear() // FIXME enable LRU cache

            if (settings.isFitToWindow) {
                width = panel.allowedWidth
            }

            val psiFile = PsiManager.getInstance(project).findFile(file) as? PsiClassOwner
            if (psiFile != null) {
                model.clear()

                if (onlyShowFailures) {
                    val projectPath = project.basePath
                    if (projectPath != null) {
                        val snapshots = LocalFileSystem.getInstance().findFileByPath(projectPath)?.children?.flatMap {
                            it.findChild("out")?.findChild("failures")?.children?.toList() ?: emptyList()
                        } ?: emptyList()

                        val packageName = psiFile.packageName
                        psiFile.classes.forEach { psiClass ->
                            val name = "delta-${packageName}_${psiClass.name}"
                            snapshots.filter {
                                it.name.startsWith(name)
                            }.forEach {
                                model.addElement(Item(it, name))
                            }
                        }
                    }
                } else {
                    val snapshots = file.getModule(project)?.rootManager?.contentRoots?.find {
                        it.name == "test"
                    }?.findChild("snapshots")?.findChild("images")?.children ?: emptyArray()

                    val packageName = psiFile.packageName
                    psiFile.classes.forEach { psiClass ->
                        val name = "${packageName}_${psiClass.name}"
                        snapshots.filter {
                            it.name.startsWith(name)
                        }.forEach {
                            model.addElement(Item(it, name))
                        }
                    }
                }
                panel?.list?.ensureIndexIsVisible(0)
            }
        }
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
