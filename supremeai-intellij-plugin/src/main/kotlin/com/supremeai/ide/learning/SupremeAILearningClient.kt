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
        private const val BACKEND_URL = "https://supremeai-565236080752.us-central1.run.app/api/v1/learning"
        private const val PLUGIN_SECRET_KEY = "supreme-ai-intellij-secret" 

        /**
         * Send error from Android Studio to backend for learning
         */
        fun sendErrorToBrain(project: Project, errorType: String, errorMessage: String, stackTrace: String) {
            try {
                val client: CloseableHttpClient = HttpClients.createDefault()
                val post = HttpPost("$BACKEND_URL/error")
                post.setHeader("Content-Type", "application/json")
                post.setHeader("X-API-Key", PLUGIN_SECRET_KEY)

                val jsonPayload = String.format(
                    "{\"errorType\": \"%s\", \"errorMessage\": \"%s\", \"severity\": \"ERROR\", \"filePath\": \"IDE\", \"codeSnippet\": \"%s\"}",
                    errorType, errorMessage.replace("\"", "\\\""), stackTrace.replace("\"", "\\\"").take(500)
                )

                post.setEntity(StringEntity(jsonPayload))
                client.execute(post)
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
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
                val post = HttpPost("$BACKEND_URL/code-edit")
                post.setHeader("Content-Type", "application/json")
                post.setHeader("X-API-Key", PLUGIN_SECRET_KEY)

                // Match backend LearningEvent DTO
                val escapedOriginal = originalCode.replace("\"", "\\\"").replace("\n", "\\n")
                val escapedEdited = editedCode.replace("\"", "\\\"").replace("\n", "\\n")
                val escapedDiff = diff.replace("\"", "\\\"").replace("\n", "\\n")
                
                val jsonPayload = """
                    {
                      "type": "CODE_EDIT",
                      "data": {
                        "taskId": "ide-edit-${System.currentTimeMillis()}",
                        "originalCode": "$escapedOriginal",
                        "editedCode": "$escapedEdited",
                        "context": "User edit in $fileName. Diff: $escapedDiff",
                        "filePath": "$filePath"
                      }
                    }
                """.trimIndent()

                post.setEntity(StringEntity(jsonPayload))
                client.execute(post)
                client.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}