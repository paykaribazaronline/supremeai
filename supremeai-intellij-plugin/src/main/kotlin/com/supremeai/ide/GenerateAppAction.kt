package com.supremeai.ide

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class GenerateAppAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val projectName = Messages.showInputDialog(
            e.project,
            "Enter the name of your new Android app",
            "SupremeAI: Generate App",
            Messages.getQuestionIcon()
        )

        if (!projectName.isNullOrBlank()) {
            Messages.showInfoMessage(
                "SupremeAI is generating '$projectName'. You will be notified when the APK is ready.",
                "Generation Started"
            )
            // Here you would call your Spring Boot API
            // Example: SupremeAIApi.generate(projectName)
        }
    }
}
