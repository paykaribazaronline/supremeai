package com.supremeai.ide.learning

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.project.ProjectManager

/**
 * Receives Gradle external-system task callbacks and forwards failures to the
 * backend learning service when a project is available.
 */
class GradleFailureDetector : ExternalSystemTaskNotificationListenerAdapter() {

    private val logger = Logger.getInstance(GradleFailureDetector::class.java)

    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
        val project = id.findProject()
        val projectPath = project?.basePath ?: "unknown"
        logger.warn("Gradle external system task failed: $projectPath", e)

        if (project == null) return

        SupremeAILearningClient.sendErrorToBrain(
            project,
            "GRADLE_BUILD_FAILURE",
            e.message ?: "Gradle task failed",
            stackTraceToString(e)
        )
    }

    override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        if (!stdOut && text.contains("FAIL", ignoreCase = true)) {
            val projectPath = id.findProject()?.basePath ?: "unknown"
            logger.info("Gradle stderr for $projectPath: ${text.take(500)}")
        }
    }

    override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {
        logger.debug("Gradle task status changed: ${event.description}")
    }

    private fun stackTraceToString(error: Exception): String {
        val writer = java.io.StringWriter()
        val printer = java.io.PrintWriter(writer)
        error.printStackTrace(printer)
        return writer.toString()
    }
}
