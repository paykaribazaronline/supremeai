package com.supremeai.ide

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class GenerateAppAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val projectName = Messages.showInputDialog(
            e.project,
            "Enter the name of your new Android app",
            "SupremeAI: Generate App",
            Messages.getQuestionIcon()
        )

        if (!projectName.isNullOrBlank()) {
            thread {
                try {
                    val url = URL("https://supremeai-565236080752.us-central1.run.app/api/project/generate")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer dev-admin-token-local")
                    conn.doOutput = true

                    val jsonInputString = "{\"projectName\": \"$projectName\", \"platform\": \"android\"}"
                    conn.outputStream.use { os ->
                        os.write(jsonInputString.toByteArray())
                    }

                    val responseCode = conn.responseCode
                    if (responseCode == 200 || responseCode == 201) {
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                            Messages.showInfoMessage(
                                "SupremeAI has started generating '$projectName'. You will be notified when it's ready.",
                                "Generation Started"
                            )
                        }
                    } else {
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                "Failed to start generation. Backend returned code: $responseCode",
                                "Error"
                            )
                        }
                    }
                } catch (ex: Exception) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            "Could not connect to SupremeAI backend: ${ex.message}",
                            "Connection Error"
                        )
                    }
                }
            }
        }
    }
}
