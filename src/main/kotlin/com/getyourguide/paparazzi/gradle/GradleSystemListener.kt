package com.getyourguide.paparazzi.gradle

import com.getyourguide.paparazzi.service.service
import java.lang.Exception
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType.RESOLVE_PROJECT
import org.jetbrains.kotlin.idea.configuration.GRADLE_SYSTEM_ID

class GradleSystemListener : ExternalSystemTaskNotificationListenerAdapter() {

    override fun onSuccess(id: ExternalSystemTaskId) {
        super.onSuccess(id)
        if (id.projectSystemId == GRADLE_SYSTEM_ID && id.type == RESOLVE_PROJECT) {
            id.findProject()?.let { project ->
                project.service.loadFromSelectedEditor(fullRefresh = true)
            }
        }
    }

    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
        if (id.projectSystemId == GRADLE_SYSTEM_ID && id.type == RESOLVE_PROJECT) {
            id.findProject()?.let { project ->
                project.service.loadFromSelectedEditor(fullRefresh = true)
            }
        }
    }
}