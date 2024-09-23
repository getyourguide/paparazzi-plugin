package com.getyourguide.paparazzi.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * Action to Zoom In the snapshots in the tool window, just like the option
 * in [ImageFileEditor][org.intellij.images.editor.ImageFileEditor]
 *
 * Not yet implemented
 */
class ZoomInAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) = Unit

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
}
