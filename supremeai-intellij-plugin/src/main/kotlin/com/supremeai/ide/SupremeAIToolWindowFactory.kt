package com.supremeai.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.net.HttpURLConnection
import java.net.URI
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel
import kotlin.concurrent.thread

class SupremeAIToolWindowFactory : ToolWindowFactory {
    companion object {
        private var chatPanel: SupremeAIChatPanel? = null
        
        fun sendToChat(message: String) {
            chatPanel?.addExternalMessage(message)
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val panel = SupremeAIChatPanel()
        chatPanel = panel
        val chatContent = contentFactory.createContent(panel.getContent(), "Chat", false)
        toolWindow.contentManager.addContent(chatContent)

        val orchestrationPanel = SupremeAIOrchestrationPanel()
        val orchestrationContent = contentFactory.createContent(orchestrationPanel.getContent(), "Orchestration", false)
        toolWindow.contentManager.addContent(orchestrationContent)

        val settingsPanel = SupremeAISettingsPanel()
        val settingsContent = contentFactory.createContent(settingsPanel.getContent(), "Settings", false)
        toolWindow.contentManager.addContent(settingsContent)
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
                    val settings = SupremeAISettings.getInstance()
                    val apiKey = settings.apiKey.trim()
                    val endpoint = settings.apiEndpoint.takeIf { it.isNotBlank() }
                        ?: "https://supremeai-a.web.app"

                    val url = URI("$endpoint/api/chat/send").toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    if (apiKey.isNotBlank()) {
                        conn.setRequestProperty("Authorization", "Bearer $apiKey")
                    }
                    conn.doOutput = true
                    
                    val jsonInputString = "{\"message\": \"$text\", \"provider\": \"google\", \"model\": \"${settings.model}\"}"
                    conn.outputStream.use { os ->
                        os.write(jsonInputString.toByteArray())
                    }

                    val responseCode = conn.responseCode
                    if (responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        SwingUtilities.invokeLater {
                            try {
                                val jsonElement = com.google.gson.JsonParser.parseString(response)
                                val aiMessage = when {
                                    jsonElement.isJsonObject -> {
                                        val json = jsonElement.asJsonObject
                                        when {
                                            json.has("message") && json.get("message").isJsonPrimitive -> json.get("message").asString
                                            json.has("response") && json.get("response").isJsonPrimitive -> json.get("response").asString
                                            json.has("text") && json.get("text").isJsonPrimitive -> json.get("text").asString
                                            json.has("content") && json.get("content").isJsonPrimitive -> json.get("content").asString
                                            json.has("reply") && json.get("reply").isJsonPrimitive -> json.get("reply").asString
                                            json.has("answer") && json.get("answer").isJsonPrimitive -> json.get("answer").asString
                                            else -> "Response received (no valid message field found)"
                                        }
                                    }
                                    jsonElement.isJsonPrimitive -> jsonElement.asString
                                    else -> "Response received (unsupported JSON format)"
                                }
                                chatArea.append("AI: ${aiMessage.replace("\r", "").trim()}\n")
                            } catch (e: Exception) {
                                chatArea.append("AI: Parse error: ${e.message?.replace("\r", "")?.trim() ?: "Unknown parse error"}\n")
                            }
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
                        ?: "https://supremeai-a.web.app"

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

        fun addExternalMessage(message: String) {
            inputField.text = message
            sendMessage()
        }
    }

    class SupremeAISettingsPanel {
        private val panel = JPanel()
        private val settings = SupremeAISettings.getInstance()

        init {
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.border = EmptyBorder(15, 15, 15, 15)

            addSection("API Configuration")

            val endpointInput = JBTextField(settings.apiEndpoint)
            endpointInput.emptyText.text = "API Endpoint URL"
            addSettingRow("Endpoint:", endpointInput)

            val apiKeyInput = JBTextField(settings.apiKey)
            apiKeyInput.emptyText.text = "API Key (optional)"
            addSettingRow("API Key:", apiKeyInput)

            val modelInput = JBTextField(settings.model)
            modelInput.emptyText.text = "AI Model"
            addSettingRow("Model:", modelInput)

            addSection("Permissions")

            addPermissionRow("Read", "read")
            addPermissionRow("Edit", "edit")
            addPermissionRow("Bash", "bash")
            addPermissionRow("Web Search", "websearch")
            addPermissionRow("External Directory", "external_directory")

            addSection("Options")

            val kimoToggle = JCheckBox("Kimo Mode", settings.kimoMode)
            kimoToggle.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            panel.add(kimoToggle)
            panel.add(Box.createVerticalStrut(6))

            val fullAuthToggle = JCheckBox("Full Authority", settings.fullAuthority)
            fullAuthToggle.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            panel.add(fullAuthToggle)
            panel.add(Box.createVerticalStrut(6))

            val shareModeCombo = JComboBox(arrayOf("manual", "auto", "disabled"))
            shareModeCombo.selectedItem = settings.shareMode
            shareModeCombo.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            addSettingRow("Share Mode:", shareModeCombo)

            addSection("Actions")

            val saveBtn = JButton("Save Settings")
            saveBtn.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            saveBtn.addActionListener {
                settings.apiEndpoint = endpointInput.text
                settings.apiKey = apiKeyInput.text
                settings.model = modelInput.text
                settings.kimoMode = kimoToggle.isSelected
                settings.fullAuthority = fullAuthToggle.isSelected
                settings.shareMode = shareModeCombo.selectedItem.toString()
                settings.save()
                JOptionPane.showMessageDialog(panel, "Settings saved successfully!")
            }
            panel.add(saveBtn)

            panel.add(Box.createVerticalGlue())
        }

        private fun addSection(title: String) {
            val label = JLabel(title)
            label.font = label.font.deriveFont(java.awt.Font.BOLD, 14f)
            label.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            panel.add(label)
            panel.add(Box.createVerticalStrut(8))
        }

        private fun addSettingRow(labelText: String, component: JComponent) {
            val row = JPanel(BorderLayout())
            row.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            row.maximumSize = java.awt.Dimension(Int.MAX_VALUE, 35)
            row.add(JLabel(labelText), BorderLayout.WEST)
            row.add(component, BorderLayout.CENTER)
            panel.add(row)
            panel.add(Box.createVerticalStrut(6))
        }

        private fun addPermissionRow(labelText: String, permissionKey: String) {
            val row = JPanel(BorderLayout())
            row.alignmentX = java.awt.Component.LEFT_ALIGNMENT
            row.maximumSize = java.awt.Dimension(Int.MAX_VALUE, 35)
            row.add(JLabel(labelText), BorderLayout.WEST)
            val combo = JComboBox(arrayOf("allow", "ask", "deny"))
            combo.selectedItem = settings.permissions[permissionKey] ?: "ask"
            combo.addActionListener {
                settings.permissions[permissionKey] = combo.selectedItem.toString()
            }
            row.add(combo, BorderLayout.CENTER)
            panel.add(row)
            panel.add(Box.createVerticalStrut(4))
        }

        fun getContent(): JPanel = panel
    }

    class SupremeAIOrchestrationPanel {
        private val panel = JPanel(BorderLayout())
        private val requirementField = JBTextField()
        private val tableModel = DefaultTableModel(arrayOf("Decision Key", "AI Consensus"), 0)
        private val resultTable = JTable(tableModel)
        private val statusLabel = JLabel("Ready")

        init {
            panel.border = EmptyBorder(10, 10, 10, 10)
            val topPanel = JPanel(BorderLayout())
            topPanel.add(JLabel("App Requirement:"), BorderLayout.NORTH)
            topPanel.add(requirementField, BorderLayout.CENTER)
            val orchestrateBtn = JButton("Orchestrate")
            orchestrateBtn.addActionListener { orchestrate() }
            topPanel.add(orchestrateBtn, BorderLayout.SOUTH)
            panel.add(topPanel, BorderLayout.NORTH)

            val centerPanel = JPanel(BorderLayout())
            centerPanel.add(JBScrollPane(resultTable), BorderLayout.CENTER)
            centerPanel.add(statusLabel, BorderLayout.SOUTH)
            panel.add(centerPanel, BorderLayout.CENTER)
        }

        private fun orchestrate() {
            val requirement = requirementField.text.trim()
            if (requirement.isEmpty()) return

            statusLabel.text = "Orchestrating..."
            tableModel.rowCount = 0
            
            thread {
                try {
                    val settings = SupremeAISettings.getInstance()
                    val apiKey = settings.apiKey.takeIf { it.isNotBlank() } ?: "dev-admin-token-local"
                    val endpoint = settings.apiEndpoint.takeIf { it.isNotBlank() }
                        ?: "https://supremeai-a.web.app"

                    val url = URI("$endpoint/api/orchestrate/requirement").toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $apiKey")
                    conn.doOutput = true

                    val jsonInputString = "{\"requirement\": \"$requirement\"}"
                    conn.outputStream.use { it.write(jsonInputString.toByteArray()) }

                    val responseCode = conn.responseCode
                    if (responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        SwingUtilities.invokeLater {
                            try {
                                val json = com.google.gson.JsonParser.parseString(response).asJsonObject
                                val status = json.get("status")?.asString ?: "Unknown"
                                val context = json.getAsJsonObject("context")
                                val decisions = context?.getAsJsonArray("decisions")
                                
                                statusLabel.text = "Status: $status"
                                decisions?.forEach { d ->
                                    val decObj = d.asJsonObject
                                    val key = decObj.get("decisionKey")?.asString ?: ""
                                    val consensus = decObj.get("aiConsensus")?.asString ?: ""
                                    tableModel.addRow(arrayOf(key, consensus))
                                }
                            } catch (ex: Exception) {
                                statusLabel.text = "Error parsing response"
                                tableModel.addRow(arrayOf("Raw Response", response))
                            }
                        }
                    } else {
                        val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $responseCode"
                        SwingUtilities.invokeLater {
                            statusLabel.text = "Failed: $error"
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = "Error: ${e.message}"
                    }
                }
            }
        }

        fun getContent(): JPanel = panel
    }
}
