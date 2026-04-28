package com.supremeai.ide.learning

import com.intellij.openapi.project.Project
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients

/**
 * Client for sending learning data from Android Studio/IntelliJ plugin to SupremeAI backend
 * Implements the "Eyes and Ears" learning mechanism described in the requirements
 */
class SupremeAILearningClient {

    companion object {
        private const val BACKEND_URL = "https://supremeai-lhlwyikwlq-uc.a.run.app/api/knowledge/failure"
        private const val PLUGIN_SECRET_KEY = "your-plugin-secret-key" // TODO: Make configurable

        /**
         * Send error from Android Studio to backend for learning
         * @param project Current IntelliJ project
         * @param errorType Type of error (e.g., "GRADLE_BUILD_FAILURE", "ANDROIDX_CONFLICT")
         * @param errorMessage Error message
         * @param stackTrace Stack trace of the error
         */
        fun sendErrorToBrain(project: Project, errorType: String, errorMessage: String, stackTrace: String) {
            try {
                val client: CloseableHttpClient = HttpClients.createDefault()
                val post = HttpPost(BACKEND_URL)
                post.setHeader("Content-Type", "application/json")
                post.setHeader("X-API-Key", PLUGIN_SECRET_KEY)

                // Create JSON data
                val escapedErrorMessage = errorMessage.replace("\"", "\\\"")
                val escapedStackTrace = stackTrace.replace("\"", "\\\"")
                val jsonPayload = String.format(
                    "{\"type\": \"ERROR\", \"category\": \"%s\", \"content\": \"%s\", \"context\": {\"stackTrace\": \"%s\", \"ide\": \"Android Studio\"}}",
                    errorType, escapedErrorMessage, escapedStackTrace
                )

                post.setEntity(StringEntity(jsonPayload))
                client.execute(post)
                
                System.out.println("🎓 SupremeAI has successfully learned from this Android Studio error!")
                
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
                System.err.println("Failed to send error to SupremeAI backend: ${e.message}")
            }
        }

        /**
         * Send successful build information for positive learning
         * @param project Current IntelliJ project
         * @param buildInfo Information about the successful build
         */
        fun sendSuccessToBrain(project: Project, buildInfo: String) {
            try {
                val client: CloseableHttpClient = HttpClients.createDefault()
                val post = HttpPost(BACKEND_URL)
                post.setHeader("Content-Type", "application/json")
                post.setHeader("X-API-Key", PLUGIN_SECRET_KEY)

                val jsonPayload = String.format(
                    "{\"type\": \"SUCCESS\", \"category\": \"BUILD_SUCCESS\", \"content\": \"%s\", \"context\": {\"ide\": \"Android Studio\"}}",
                    buildInfo.replace("\"", "\\\"")
                )

                post.setEntity(StringEntity(jsonPayload))
                client.execute(post)
                
                System.out.println("🎓 SupremeAI has learned from successful build!")
                
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
                System.err.println("Failed to send success to SupremeAI backend: ${e.message}")
            }
        }

        /**
         * Send code edit information for learning from user modifications
         * @param project Current IntelliJ project
         * @param fileName Name of the file being edited
         * @param originalCode Original code before user edit
         * @param editedCode Code after user edit
         * @param diff Description of changes made
         * @param filePath Full path to the file
         */
        fun sendCodeEditToBrain(
            project: Project,
            fileName: String,
            originalCode: String,
            editedCode: String,
            diff: String,
            filePath: String
        ) {
            try {
                val client: CloseableHttpClient = HttpClients.createDefault()
                val post = HttpPost(BACKEND_URL)
                post.setHeader("Content-Type", "application/json")
                post.setHeader("X-API-Key", PLUGIN_SECRET_KEY)

                // Create JSON data for code edit learning
                val escapedOriginal = originalCode.replace("\"", "\\\"")
                val escapedEdited = editedCode.replace("\"", "\\\"")
                val escapedDiff = diff.replace("\"", "\\\"")
                val jsonPayload = String.format(
                    "{\"type\": \"CODE_EDIT\", \"category\": \"USER_EDIT\", \"content\": \"User edited %s\", \"context\": {\"fileName\": \"%s\", \"originalCode\": \"%s\", \"editedCode\": \"%s\", \"diff\": \"%s\", \"filePath\": \"%s\", \"ide\": \"Android Studio\"}}",
                    fileName, fileName, escapedOriginal, escapedEdited, escapedDiff, filePath
                )

                post.setEntity(StringEntity(jsonPayload))
                client.execute(post)
                
                System.out.println("🎓 SupremeAI has learned from user code edit in $fileName")
                
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
                System.err.println("Failed to send code edit to SupremeAI backend: ${e.message}")
            }
        }
    }
}