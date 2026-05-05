package com.supremeai.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.Consumer
import java.awt.event.MouseEvent

class SupremeAIStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "SupremeAIStatusBar"
    override fun getDisplayName(): String = "SupremeAI"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = SupremeAIStatusBarWidget(project)
    override fun canBeEnabledOn(statusBar: com.intellij.openapi.wm.StatusBar): Boolean = true

    class SupremeAIStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.IconPresentation {
        override fun ID(): String = "SupremeAIStatusBar"
        override fun getPresentation(): StatusBarWidget.WidgetPresentation = this
        override fun getTooltipText(): String = "Open SupremeAI Chat"
        override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
            ToolWindowManager.getInstance(project).getToolWindow("SupremeAI")?.show()
        }
        override fun getIcon(): javax.swing.Icon = com.intellij.icons.AllIcons.General.Information
    }
}
