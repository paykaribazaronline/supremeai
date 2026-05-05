package com.supremeai.ide

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager

class ExplainFileAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("SupremeAI")
        toolWindow?.show {
            SupremeAIToolWindowFactory.sendToChat("Explain the file: ${virtualFile.path}")
        }
    }

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile != null && !virtualFile.isDirectory
    }
}
