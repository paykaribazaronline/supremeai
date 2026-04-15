package com.supremeai.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.concurrent.thread

class SupremeAIToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val chatPanel = SupremeAIChatPanel()
        val chatContent = contentFactory.createContent(chatPanel.getContent(), "Chat", false)
        toolWindow.contentManager.addContent(chatContent)

        val agentsPanel = SupremeAIAgentsPanel()
        val agentsContent = contentFactory.createContent(agentsPanel.getContent(), "Agents", false)
        toolWindow.contentManager.addContent(agentsContent)

        val projectsPanel = SupremeAIProjectsPanel()
        val projectsContent = contentFactory.createContent(projectsPanel.getContent(), "Projects", false)
        toolWindow.contentManager.addContent(projectsContent)
    }

    class SupremeAIChatPanel {
        private val panel = JPanel(BorderLayout())
        private val chatArea = JTextArea()
        private val inputField = JBTextField()
        private val statusLabel = JLabel("● Backend: Connecting...")

        init {
            setupUI()
            checkBackendStatus()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            // Header
            val header = JPanel(BorderLayout())
            header.add(JLabel("SupremeAI Assistant"), BorderLayout.WEST)
            header.add(statusLabel, BorderLayout.EAST)
            panel.add(header, BorderLayout.NORTH)

            // Chat Area
            chatArea.isEditable = false
            chatArea.lineWrap = true
            chatArea.wrapStyleWord = true
            val scrollPane = JBScrollPane(chatArea)
            panel.add(scrollPane, BorderLayout.CENTER)

            // Input Area
            val inputPanel = JPanel(BorderLayout())
            inputPanel.add(inputField, BorderLayout.CENTER)
            val sendBtn = JButton("Send")
            sendBtn.addActionListener { sendMessage() }
            inputPanel.add(sendBtn, BorderLayout.EAST)
            panel.add(inputPanel, BorderLayout.SOUTH)

            inputField.addActionListener { sendMessage() }
        }

        private fun sendMessage() {
            val text = inputField.text.trim()
            if (text.isEmpty()) return
            
            chatArea.append("You: $text\n")
            inputField.text = ""

            thread {
                try {
                    val url = URL("https://supremeai-565236080752.us-central1.run.app/api/chat/send")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer dev-admin-token-local")
                    conn.doOutput = true
                    
                    val jsonInputString = "{\"message\": \"$text\", \"provider\": \"meta-llama\"}"
                    conn.outputStream.use { os ->
                        os.write(jsonInputString.toByteArray())
                    }

                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    SwingUtilities.invokeLater {
                        chatArea.append("AI: Response received from Llama 3\n")
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        chatArea.append("AI: [Offline] Could not connect to backend.\n")
                    }
                }
            }
        }

        private fun checkBackendStatus() {
            thread {
                try {
                    val url = URL("https://supremeai-565236080752.us-central1.run.app/api/status/check")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    if (conn.responseCode == 200) {
                        SwingUtilities.invokeLater {
                            statusLabel.text = "● Backend: Online"
                            statusLabel.foreground = java.awt.Color.GREEN
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = "● Backend: Offline"
                        statusLabel.foreground = java.awt.Color.RED
                    }
                }
            }
        }

        fun getContent(): JPanel = panel
    }

    class SupremeAIAgentsPanel {
        private val panel = JPanel(BorderLayout())
        init {
            val model = DefaultListModel<String>()
            model.addElement("X-Builder - Active")
            model.addElement("Y-Reviewer - Waiting")
            model.addElement("Z-Architect - Standby")
            val list = JList(model)
            panel.add(JBScrollPane(list), BorderLayout.CENTER)
            panel.add(JLabel("Active AI Agents"), BorderLayout.NORTH)
        }
        fun getContent(): JPanel = panel
    }

    class SupremeAIProjectsPanel {
        private val panel = JPanel(BorderLayout())
        init {
            val model = DefaultListModel<String>()
            model.addElement("SupremeAI Mobile (Android)")
            model.addElement("SupremeAI Web (React)")
            model.addElement("Admin Dashboard (Spring Boot)")
            val list = JList(model)
            panel.add(JBScrollPane(list), BorderLayout.CENTER)
            panel.add(JLabel("Managed Projects"), BorderLayout.NORTH)
        }
        fun getContent(): JPanel = panel
    }
}
