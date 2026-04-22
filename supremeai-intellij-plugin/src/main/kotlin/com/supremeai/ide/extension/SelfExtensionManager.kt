package com.supremeai.ide.extension

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import java.io.File

@Service(Service.Level.PROJECT)
class SelfExtensionManager(private val project: Project) {

    fun deployNewService(serviceName: String, code: String) {
        val basePath = project.basePath ?: return
        val targetDir = File(basePath, "src/main/kotlin/com/supremeai/generated")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val serviceFile = File(targetDir, "${serviceName}.kt")
        serviceFile.writeText(code)

        // Refresh VFS to let IDE know about the new file
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(serviceFile)

        // Automate Git commit for the new extension
        GitAutomationManager.getInstance(project).commitAndPush("Autonomous extension: Added $serviceName")
    }

    companion object {
        fun getInstance(project: Project) = project.service<SelfExtensionManager>()
    }
}
