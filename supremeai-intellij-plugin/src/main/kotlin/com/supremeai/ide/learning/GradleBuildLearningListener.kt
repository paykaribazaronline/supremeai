package com.supremeai.ide.learning

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionInfo
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import java.util.regex.Pattern

/**
 * Listens for Gradle build executions to capture failures and successes for SupremeAI learning.
 * Implements the "Ear" component of the Android Studio plugin learning system.
 */
class GradleBuildLearningListener : ExecutionListener {

    // Pattern to identify Gradle build processes
    private val GRADLE_PATTERN = Pattern.compile(".*Gradle.*Build.*", Pattern.CASE_INSENSITIVE)
    private val GRADLE_COMMAND_PATTERN = Pattern.compile(".*gradlew.*|.*gradle.*", Pattern.CASE_INSENSITIVE)

    override fun onExecutionStart(executionInfo: ExecutionInfo?) {
        // No action needed on start
    }

    override fun onExecutionFinish(executionInfo: ExecutionInfo?, executionResult: ExecutionResult) {
        val project = executionInfo?.project
        if (project == null) return

        // Check if this is a Gradle build execution
        if (!isGradleBuild(executionInfo)) return

        val exitCode = executionResult.exitCode
        val stdout = executionResult.stdout ?: ""
        val stderr = executionResult.stderr ?: ""
        val fullOutput = "$stdout\n$stderr".trim()

        // Only process if we have meaningful output or non-zero exit code
        if (exitCode != 0 || StringUtil.isNotEmpty(fullOutput)) {
            if (exitCode != 0) {
                // Build failed - extract error information
                val errorMessage = extractErrorMessage(fullOutput)
                val stackTrace = extractStackTrace(fullOutput)
                
                SupremeAILearningClient.sendErrorToBrain(
                    project,
                    "GRADLE_BUILD_FAILURE",
                    errorMessage,
                    stackTrace
                )
            } else {
                // Build succeeded - optionally log success for learning
                // Uncomment if you want to learn from successful builds too
                // SupremeAILearningClient.sendSuccessToBrain(project, "Gradle build succeeded")
            }
        }
    }

    /**
     * Determines if the execution represents a Gradle build process.
     */
    private fun isGradleBuild(executionInfo: ExecutionInfo?): Boolean {
        if (executionInfo == null) return false

        // Check the executable name
        val executable = executionInfo.executable?.toLowerCase() ?: ""
        if (GRADLE_COMMAND_PATTERN.matcher(executable).matches()) return true

        // Check the command line arguments
        val commandLine = executionInfo.commandLine?.toLowerCase() ?: ""
        if (GRADLE_COMMAND_PATTERN.matcher(commandLine).matches()) return true

        // Check the environment or other properties if needed
        val runProfile = executionInfo.runProfile
        if (runProfile != null) {
            val profileName = runProfile.name.toLowerCase()
            if (GRADLE_PATTERN.matcher(profileName).matches()) return true
        }

        return false
    }

    /**
     * Extracts the main error message from build output.
     */
    private fun extractErrorMessage(output: String): String {
        // Look for common error patterns in Gradle output
        val errorLines = output.lines().filter { line ->
            line.contains("error:", ignoreCase = true) ||
                    line.contains("FAILURE:", ignoreCase = true) ||
                    line.contains("Execution failed", ignoreCase = true) ||
                    line.contains("> Task", ignoreCase = true) && line.contains("FAILED", ignoreCase = true)
        }.toList()

        return if (errorLines.isNotEmpty()) {
            // Take the first few error lines
            errorLines.take(3).joinToString("\n")
        } else {
            // Fallback: last 500 characters of output
            output.takeLast(500)
        }
    }

    /**
     * Extracts stack trace from build output if available.
     */
    private fun extractStackTrace(output: String): String {
        val stackTraceLines = output.lines().filter { line ->
            line.contains("at ", ignoreCase = true) &&
                    !line.contains("supremeai", ignoreCase = true) && // Filter out our own plugin
                    line.trim().startsWith("at")
        }.toList()

        return if (stackTraceLines.isNotEmpty()) {
            stackTraceLines.take(10).joinToString("\n") // Limit stack trace length
        } else {
            "No stack trace available"
        }
    }
}