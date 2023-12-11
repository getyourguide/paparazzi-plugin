package com.getyourguide.paparazzi.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class ProjectStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        project.service.loadFromSelectedEditor(true)
    }
}
