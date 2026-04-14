package com.supremeai.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.BoxLayout

class SupremeAIToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = SupremeAIToolWindow()
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }

    class SupremeAIToolWindow {
        private val panel = JPanel()

        init {
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.add(JLabel("SupremeAI Agents"))
            panel.add(JButton("X-Builder (Active)"))
            panel.add(JButton("Y-Reviewer (Waiting)"))
            panel.add(JButton("Z-Architect (Standby)"))
        }

        fun getContent(): JPanel = panel
    }
}
