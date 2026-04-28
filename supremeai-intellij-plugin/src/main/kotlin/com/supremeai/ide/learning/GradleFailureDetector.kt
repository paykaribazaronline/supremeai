package com.supremeai.ide.learning

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionInfo
import com.intellij.execution.ExecutionResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.events.ActionExecutedDetails
import org.gradle.tooling.tasks.BuildActionExecuter
import org.gradle.tooling.tasks.BuildFinishedListener
import org.gradle.tooling.tasks.TaskExecutionListener
import org.gradle.tooling.tasks.TaskFailureListener
import org.gradle.tooling.tasks.TaskSuccessListener
import java.util.regex.Pattern

/**
 * Listens for Gradle build executions using Gradle tooling API to capture failures and successes.
 * This implements the ExternalSystemTaskNotificationListener referenced in plugin.xml
 * but uses Gradle's own task execution listeners for more precise Gradle build monitoring.
 */
class GradleFailureDetector : 
    TaskExecutionListener,
    TaskSuccessListener,
    TaskFailureListener,
    BuildFinishedListener {

    // Pattern to identify relevant Gradle tasks
    private val BUILD_TASK_PATTERN = Pattern.compile(".*:assemble.*|.*:build.*|.*:compile.*", Pattern.CASE_INSENSITIVE)
    private val PROJECT_SYNC_PATTERN = Pattern.compile(".*:generate.*|.*:processResources.*", Pattern.CASE_INSENSITIVE)

    private lateinit var project: Project
    private lateinit var buildLauncher: BuildLauncher

    constructor(project: Project, buildLauncher: BuildLauncher) {
        this.project = project
        this.buildLauncher = buildLauncher
    }

    // Called before task execution
    override fun beforeTask(task: BuildActionExecuter) {
        // No action needed before task
    }

    // Called after task execution (regardless of success/failure)
    override fun afterTask(task: BuildActionExecuter, taskState: org.gradle.tooling.events.TaskState) {
        val taskName = task.name
        val taskPath = task.path
        
        // Check if this is a build-related task
        if (isBuildTask(taskName) || isBuildTask(taskPath)) {
            if (taskState.failure != null) {
                // Task failed
                val failure = taskState.failure!!
                val errorMessage = failure.message ?: "Unknown error"
                val stackTrace = getStackTraceString(failure)
                
                SupremeAILearningClient.sendErrorToBrain(
                    project,
                    "GRADLE_TASK_FAILURE",
                    "Task '$taskName' failed: $errorMessage",
                    stackTrace
                )
            } else {
                // Task succeeded - optionally log for learning
                // SupremeAILearningClient.sendSuccessToBrain(project, "Task '$taskName' succeeded")
            }
        }
    }

    // Called when task succeeds
    override fun onSuccess(task: BuildActionExecuter) {
        // Success already handled in afterTask
    }

    // Called when task fails
    override fun onFailure(task: BuildActionExecuter, failure: Throwable) {
        // Failure already handled in afterTask
    }

    // Called when build finishes
    override fun buildFinished(result: org.gradle.tooling.events.BuildResult) {
        if (result.failure != null) {
            // Overall build failed
            val failure = result.failure!!
            val errorMessage = failure.message ?: "Build failed"
            val stackTrace = getStackTraceString(failure)
            
            SupremeAILearningClient.sendErrorToBrain(
                project,
                "GRADLE_BUILD_FAILURE",
                errorMessage,
                stackTrace
            )
        } else {
            // Build succeeded
            SupremeAILearningClient.sendSuccessToBrain(
                project,
                "Gradle build completed successfully"
            )
        }
    }

    /**
     * Determines if a task name/path represents a build task.
     */
    private fun isBuildTask(taskIdentifier: String): Boolean {
        if (taskIdentifier.isNullOrBlank()) return false
        
        val lowerCaseIdentifier = taskIdentifier.lowercase()
        return BUILD_TASK_PATTERN.matcher(lowerCaseIdentifier).matches() ||
               PROJECT_SYNC_PATTERN.matcher(lowerCaseIdentifier).matches()
    }

    /**
     * Converts a Throwable to a stack trace string.
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val stackTraceElements = throwable.stackTrace.takeWhile { element ->
            !element.className.startsWith("org.gradle") &&
                    !element.className.startsWith("java.lang") &&
                    !element.className.startsWith("sun.reflect") &&
                    !element.className.contains("supremeai", ignoreCase = true)
        }
        
        if (stackTraceElements.isNotEmpty()) {
            val sw = java.io.StringWriter()
            val pw = java.io.PrintWriter(sw)
            throwable.printStackTrace(pw)
            return sw.toString()
        } else {
            // Fallback to limited stack trace if all are filtered out
            val stackTrace = throwable.stackTrace.take(5).joinToString("\n") { 
                "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})"
            }
            return "${throwable::class.simpleName}: ${throwable.message}\n$stackTrace"
        }
    }
}