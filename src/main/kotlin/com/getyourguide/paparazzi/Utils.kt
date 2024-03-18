package com.getyourguide.paparazzi

import com.getyourguide.paparazzi.service.Snapshot
import com.intellij.lang.jvm.annotation.JvmAnnotationClassValue
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.asJava.elements.KtLightPsiClassObjectAccessExpression
import org.jetbrains.kotlin.asJava.elements.KtLightPsiNameValuePair
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.util.projectStructure.getModule
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.plugins.gradle.util.GradleUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElement
import java.io.File
import java.util.concurrent.Callable

private const val PAPARAZZI_IMPORT = "app.cash.paparazzi.Paparazzi"
private const val PARAMETRIZED_CLASS_NAME = "org.junit.runners.Parameterized"
private const val RUN_WITH_CLASS_NAME = "org.junit.runner.RunWith"
private const val TEST_PARAM_INJECTOR_CLASS_NAME =
    "com.google.testing.junit.testparameterinjector.TestParameterInjector"

internal fun VirtualFile.toSnapshots(project: Project, isFailure: Boolean): List<Snapshot> {
    val psiFile = PsiManager.getInstance(project).findFile(this) as? PsiClassOwner
    return if (psiFile != null) {
        val snapshots = if (isFailure) project.failureDiffSnapshots(this) else project.recordedSnapshots(this)
        psiFile.toSnapshots(snapshots, isFailure)
    } else emptyList()
}

internal fun Project.isToolWindowOpened(): Boolean {
    return ToolWindowManager.getInstance(this).getToolWindow(TOOL_WINDOW_ID)?.isVisible == true
}

internal fun PsiElement.containingMethod(): UMethod? {
    return this.parents.toList().map { it.toUElement() }.find { it is UMethod } as? UMethod
}

internal val FileEditor.caretModel: CaretModel?
    get() = (this as? TextEditor)?.editor?.caretModel

internal fun VirtualFile.psiElement(project: Project, offset: Int): PsiElement? {
    val psiFile = PsiManager.getInstance(project).findFile(this)
    return psiFile?.findElementAt(offset)
}

internal fun PsiElement.file(): VirtualFile? = containingFile.virtualFile

internal fun VirtualFile.methods(project: Project): List<String> {
    val psiClassOwner = PsiManager.getInstance(project).findFile(this) as? PsiClassOwner
    return psiClassOwner?.methods() ?: emptyList()
}

internal fun Project.modulePath(file: VirtualFile): String? {
    return gradleModuleData(file)?.data?.linkedExternalProjectPath ?: basePath?.let { projectPath ->
        val relativePath = FileUtil.getRelativePath(projectPath, file.path, File.separatorChar)
        val moduleName = relativePath?.split(File.separator)?.firstOrNull()
        if (moduleName != null) projectPath + File.separator + moduleName else null
    }
}

@Suppress("UnstableApiUsage")
internal fun Project.gradleModuleData(
    file: VirtualFile,
): DataNode<ModuleData>? {
    return file.getModule(this)?.let { module ->
        return GradleUtil.findGradleModuleData(module)
    }
}

@Suppress("UnstableApiUsage")
internal fun gradleModuleData(
    dir: PsiDirectory,
): DataNode<ModuleData>? {
    return dir.module?.let { module ->
        return GradleUtil.findGradleModuleData(module)
    }
}

internal fun DataNode<ModuleData>.getProjectPath(): String? {
    val parent = parent
    return if (parent != null) {
        when (val data = parent.data) {
            is ModuleData -> {
                @Suppress("UNCHECKED_CAST")
                (parent as DataNode<ModuleData>).getProjectPath()
            }

            is ProjectData -> {
                data.ideProjectFileDirectoryPath
            }

            else -> null
        }
    } else {
        data.linkedExternalProjectPath
    }
}

internal fun DataNode<ModuleData>.getModuleId(): String? {
    return if (getProjectPath() == data.linkedExternalProjectPath) {
        null
    } else {
        data.id
    }
}

internal inline fun <reified T> nonBlocking(crossinline asyncAction: () -> T, crossinline uiThreadAction: (T) -> Unit) {
    ReadAction.nonBlocking(Callable {
        asyncAction()
    }).finishOnUiThread(ModalityState.defaultModalityState()) { T ->
        uiThreadAction(T)
    }.submit(AppExecutorUtil.getAppExecutorService())
}

internal fun UClass.isPaparazziClass(): Boolean {
    return fields.any { field -> field.isPaparazziField() } || javaPsi.supers.any { superClass ->
        superClass.fields.any { field -> field.isPaparazziField() }
    }
}

internal fun PsiElement.containingUClass(): UClass? {
    return (parents.toList().map { it.toUElement() }.find { it is UClass } as? UClass)
}

internal fun isParametrizedTest(
    psiClass: PsiClass,
    psiMethod: PsiMethod?,
): Boolean {
    if (psiMethod != null && psiMethod.hasParameters()) {
        // Most likely it uses some parametrization
        return true
    }
    val annotation = psiClass.annotations.find {
        RUN_WITH_CLASS_NAME == it.qualifiedName
    } ?: return false
    val pair = annotation.attributes.find { it.attributeName == "value" } ?: return false
    val qualifiedName = if (psiClass.language is KotlinLanguage) {
        val kotlinPair = pair as KtLightPsiNameValuePair
        val runnerClass = kotlinPair.value as KtLightPsiClassObjectAccessExpression
        val runnerType = runnerClass.type as PsiClassReferenceType
        runnerType.reference.qualifiedName

    } else {
        val classValue = pair.attributeValue as JvmAnnotationClassValue
        classValue.qualifiedName
    }
    return PARAMETRIZED_CLASS_NAME == qualifiedName || TEST_PARAM_INJECTOR_CLASS_NAME == qualifiedName
}

private fun PsiClassOwner.toSnapshots(snapshots: List<VirtualFile>, isFailure: Boolean): List<Snapshot> {
    val prefix = if (isFailure) "delta-" else ""
    return classes.flatMap { psiClass ->
        val name = "$prefix${packageName}_${psiClass.name}"
        snapshots.filter { it.name.startsWith(name) }.map {
            Snapshot(it, it.snapshotName(name))
        }
    }
}

private fun PsiClassOwner.methods(): List<String> {
    return classes.flatMap { psiClass ->
        psiClass.methods.map { it.name }
    }
}

private fun VirtualFile.snapshotName(qualifiedTestName: String): String {
    return nameWithoutExtension.substringAfter(qualifiedTestName + "_")
}

private fun Project.recordedSnapshots(file: VirtualFile): List<VirtualFile> {
    val modulePath = modulePath(file)
    return if (modulePath != null) {
        LocalFileSystem.getInstance().findFileByPath(modulePath)?.findChild("src")?.findChild("test")
    } else {
        file.getModule(this)?.rootManager?.contentRoots?.find { it.name == "test" }
    }?.findChild("snapshots")?.findChild("images")?.children?.toList() ?: emptyList()
}

private fun Project.failureDiffSnapshots(file: VirtualFile): List<VirtualFile> {
    val modulePath = modulePath(file)
    val projectPath = basePath
    return if (modulePath != null) {
        LocalFileSystem.getInstance().findFileByPath(modulePath)?.findChild("out")
            ?.findChild("failures")?.children?.toList() ?: emptyList()
    } else if (projectPath != null) {
        LocalFileSystem.getInstance().findFileByPath(projectPath)?.children?.flatMap {
            it.findChild("out")?.findChild("failures")?.children?.toList() ?: emptyList()
        } ?: emptyList()
    } else emptyList()
}

private fun PsiField.isPaparazziField(): Boolean {
    val psiClass = PsiTypesUtil.getPsiClass(type)
    return PAPARAZZI_IMPORT == psiClass?.qualifiedName ||
            psiClass?.supers?.any { parentClass ->
                PAPARAZZI_IMPORT == parentClass.qualifiedName
            } == true
}