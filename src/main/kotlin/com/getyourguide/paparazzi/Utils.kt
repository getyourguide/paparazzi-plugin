package com.getyourguide.paparazzi

import com.getyourguide.paparazzi.service.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction
import org.jetbrains.plugins.gradle.service.project.GradleTasksIndices

internal fun Project.loadFromSelectedEditorFile() {
    val file = FileEditorManager.getInstance(this)?.selectedEditor?.file
    if (file != null) {
        service.reload(file)
    }
}

// TODO add param to run for TestClass, or TestMethod
internal fun Project.runRecordPaparazzi(modulePath: String) {
    // To find the list of tasks. We need to show a UI to select from different build variant
    val tasks = GradleTasksIndices.getInstance(this).findTasks(modulePath, "recordPaparazzi")

    // To execute the gradle command
    GradleExecuteTaskAction.runGradle(this, null, modulePath, "recordPaparazziDebug")
}
