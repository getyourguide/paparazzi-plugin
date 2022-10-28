package com.madrapps.paparazzi.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.madrapps.paparazzi.PaparazziWindowPanel
import org.jetbrains.kotlin.idea.util.application.getService

interface MainService {

    class Storage {
        // path to snapshots
        // no of screenshots to show at a time (everything at once can cause OOM)
    }

    var panel: PaparazziWindowPanel?
}

@State(name = "com.madrapps.paparazzi", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class MainServiceImpl(private val project: Project) : MainService, PersistentStateComponent<MainService.Storage> {

    private var storage = MainService.Storage()
    override var panel: PaparazziWindowPanel? = null

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