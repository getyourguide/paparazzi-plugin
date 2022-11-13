package com.getyourguide.paparazzi.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.startup.StartupManager

class ProjectStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        StartupManager.getInstance(project).runAfterOpened {
            project.service.loadFromSelectedEditor(true)
        }
    }
}
