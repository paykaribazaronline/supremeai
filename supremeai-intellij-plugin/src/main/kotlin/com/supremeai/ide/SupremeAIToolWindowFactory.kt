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
import java.net.URI
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
            val titlePanel = JPanel(BorderLayout())
            titlePanel.add(JLabel("SupremeAI Assistant"), BorderLayout.WEST)
            titlePanel.add(statusLabel, BorderLayout.EAST)
            header.add(titlePanel, BorderLayout.NORTH)

            // API Key Setting in Tool Window
            val settingsPanel = JPanel(BorderLayout())
            settingsPanel.border = EmptyBorder(5, 0, 5, 0)
            val apiKeyInput = JBTextField(SupremeAISettings.getInstance().apiKey)
            apiKeyInput.emptyText.text = "Enter API Key..."
            settingsPanel.add(JLabel("API Key: "), BorderLayout.WEST)
            settingsPanel.add(apiKeyInput, BorderLayout.CENTER)
            val saveBtn = JButton("Set")
            saveBtn.addActionListener {
                SupremeAISettings.getInstance().apiKey = apiKeyInput.text
                statusLabel.text = "● Backend: Updating..."
                checkBackendStatus()
            }
            settingsPanel.add(saveBtn, BorderLayout.EAST)
            header.add(settingsPanel, BorderLayout.SOUTH)

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
                    val settings = SupremeAISettings.getInstance()
                    val apiKey = settings.apiKey.takeIf { it.isNotBlank() } ?: "dev-admin-token-local"
                    val endpoint = settings.apiEndpoint.takeIf { it.isNotBlank() }
                        ?: "https://supremeai-565236080752.us-central1.run.app"

                    val url = URI("$endpoint/api/chat/send").toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $apiKey")
                    conn.doOutput = true
                    
                    val jsonInputString = "{\"message\": \"$text\", \"provider\": \"meta-llama\"}"
                    conn.outputStream.use { os ->
                        os.write(jsonInputString.toByteArray())
                    }

                    val responseCode = conn.responseCode
                    if (responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        SwingUtilities.invokeLater {
                            chatArea.append("AI: Response received\n")
                        }
                    } else {
                        val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                        SwingUtilities.invokeLater {
                            chatArea.append("AI: [Error $responseCode] $errorResponse\n")
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        chatArea.append("AI: [Offline] Connection failed: ${e.message}\n")
                    }
                }
            }
        }

        private fun checkBackendStatus() {
            thread {
                try {
                    val settings = SupremeAISettings.getInstance()
                    val endpoint = settings.apiEndpoint.takeIf { it.isNotBlank() }
                        ?: "https://supremeai-565236080752.us-central1.run.app"

                    val url = URI("$endpoint/api/status").toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"
                    val responseCode = conn.responseCode
                    // 200 is success, 401 means server is up but needs auth (which is fine for a status check)
                    if (responseCode == 200 || responseCode == 401) {
                        SwingUtilities.invokeLater {
                            statusLabel.text = "● Backend: Online"
                            statusLabel.foreground = java.awt.Color.GREEN
                        }
                    } else {
                        SwingUtilities.invokeLater {
                            statusLabel.text = "● Backend: Error ($responseCode)"
                            statusLabel.foreground = java.awt.Color.ORANGE
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
