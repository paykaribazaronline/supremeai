package com.supremeai.ide.extension

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File

@Service(Service.Level.PROJECT)
class GitAutomationManager(private val project: Project, private val scope: CoroutineScope) {

    fun commitAndPush(message: String) {
        scope.launch(Dispatchers.IO) {
            val basePath = project.basePath ?: return@launch
            runCommand(listOf("git", "add", "."), basePath)
            runCommand(listOf("git", "commit", "-m", message), basePath)
            runCommand(listOf("git", "push"), basePath)
        }
    }

    private fun runCommand(command: List<String>, workingDir: String) {
        try {
            val process = ProcessBuilder(command)
                .directory(File(workingDir))
                .redirectErrorStream(true)
                .start()
            
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()
            println("Git command ${command.joinToString(" ")} output: $output")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun getInstance(project: Project) = project.service<GitAutomationManager>()
    }
}
