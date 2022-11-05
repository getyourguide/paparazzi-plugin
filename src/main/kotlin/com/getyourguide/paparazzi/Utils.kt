package com.getyourguide.paparazzi

import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

internal fun Project.loadFromSelectedEditorFile() {
    val file = FileEditorManager.getInstance(this)?.selectedEditor?.file
    if (file != null) {
        service.reload(file)
    }
}
