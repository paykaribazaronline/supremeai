package com.supremeai.ide

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridLayout
import java.net.HttpURLConnection
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel
import kotlin.concurrent.thread

class SupremeAIToolWindowFactory : ToolWindowFactory {
    companion object {
        var chatPanel: SupremeAIChatPanel? = null
            private set
        var dashboardPanel: SupremeAIDashboardPanel? = null
            private set
        var activityPanel: SupremeAIActivityPanel? = null
            private set
        
        fun sendToChat(message: String) {
            chatPanel?.addExternalMessage(message)
        }
        
        fun refreshDashboard() {
            dashboardPanel?.refresh()
        }
        
        fun refreshActivity() {
            activityPanel?.refresh()
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        // Dashboard Tab
        val dashboardContent = contentFactory.createContent(SupremeAIDashboardPanel(project).getContent(), "Dashboard", false)
        dashboardContent.icon = null // Can set icon here
        toolWindow.contentManager.addContent(dashboardContent)

        // Chat Tab
        val chatContent = contentFactory.createContent(SupremeAIChatPanel().getContent(), "Chat", false)
        toolWindow.contentManager.addContent(chatContent)

        // Activity Tab
        val activityContent = contentFactory.createContent(SupremeAIActivityPanel().getContent(), "Activity", false)
        toolWindow.contentManager.addContent(activityContent)

        // Orchestration Tab
        val orchestrationPanel = SupremeAIOrchestrationPanel()
        val orchestrationContent = contentFactory.createContent(orchestrationPanel.getContent(), "Orchestration", false)
        toolWindow.contentManager.addContent(orchestrationContent)

        // Settings Tab
        val settingsPanel = SupremeAISettingsPanel()
        val settingsContent = contentFactory.createContent(settingsPanel.getContent(), "Settings", false)
        toolWindow.contentManager.addContent(settingsContent)
    }

    class SupremeAIChatPanel {
        private val panel = JPanel(BorderLayout())
        private val chatArea = JTextArea()
        private val inputField = JBTextField()
        private val statusLabel = JLabel("● Backend: Connecting...")
        private val modeLabel = JLabel("Mode: Code")

        init {
            setupUI()
            checkBackendStatus()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            // Header
            val header = JPanel(BorderLayout())
            val leftHeader = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0))
            leftHeader.add(JLabel("SupremeAI Assistant"))
            modeLabel.border = EmptyBorder(0, 10, 0, 0)
            modeLabel.foreground = java.awt.Color.GRAY
            leftHeader.add(modeLabel)
            
            header.add(leftHeader, BorderLayout.WEST)
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
                                val gson = com.google.gson.Gson()
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
                                
                                val detectedMode = if (jsonElement.isJsonObject) {
                                    jsonElement.asJsonObject.get("mode")?.asString
                                } else null
                                
                                SwingUtilities.invokeLater {
                                    chatArea.append("AI: ${aiMessage.replace("\r", "").trim()}\n")
                                    detectedMode?.let { 
                                        modeLabel.text = "Mode: ${it.replaceFirstChar { c -> c.uppercase() }}"
                                        modeLabel.foreground = java.awt.Color.BLUE
                                    }
                                }
                            } catch (e: Exception) {
                                chatArea.append("AI: Parse error: ${e.message?.replace("\r", "")?.trim() ?: "Unknown parse error"}\n")
                            }
                        }
                     } else {
                         val errorResponse = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                         SwingUtilities.invokeLater {
                             when (responseCode) {
                                 401 -> {
                                     val settings = SupremeAISettings.getInstance()
                                     val apiKeyConfigured = settings.apiKey.trim().isNotBlank()
                                     val endpointConfigured = settings.apiEndpoint.trim().isNotBlank()

                                     val authMessage = buildString {
                                         append("AI: 🔐 Authentication Required\n")
                                         append("To chat with SupremeAI, you need to configure your credentials:\n")
                                         if (!apiKeyConfigured) {
                                             append("• API Key is not configured\n")
                                         }
                                         if (!endpointConfigured) {
                                             append("• API Endpoint is not configured (using default)\n")
                                         }
                                         append("\nPlease go to the Settings tab to configure these options.\n")
                                         append("Once configured, try sending your message again.\n")
                                     }
                                     chatArea.append(authMessage)

                                     // Auto-focus settings tab
                                     try {
                                         val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(com.intellij.openapi.project.ProjectManager.getInstance().defaultProject)
                                         val toolWindow = toolWindowManager.getToolWindow("SupremeAI")
                                         if (toolWindow != null) {
                                             val settingsContent = toolWindow.contentManager.contents.find { it.tabName == "Settings" }
                                             if (settingsContent != null) {
                                                 toolWindow.contentManager.setSelectedContent(settingsContent)
                                                 toolWindow.show()
                                             }
                                         }
                                     } catch (e: Exception) {
                                         // Ignore if we can't switch tabs
                                     }
                                 }
                                 403 -> {
                                     chatArea.append("AI: 🚫 Access Denied\n")
                                     chatArea.append("You don't have permission to access this feature.\n")
                                     chatArea.append("Please contact your administrator or check your account permissions.\n")
                                 }
                                 429 -> {
                                     chatArea.append("AI: ⏱️ Rate Limited\n")
                                     chatArea.append("Too many requests. Please wait a moment before trying again.\n")
                                 }
                                 500, 502, 503, 504 -> {
                                     chatArea.append("AI: 🚨 Server Error\n")
                                     chatArea.append("The SupremeAI service is currently experiencing issues.\n")
                                     chatArea.append("Please try again in a few minutes.\n")
                                 }
                                 else -> {
                                     chatArea.append("AI: [Error $responseCode] $errorResponse\n")
                                 }
                             }
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
                    // 200 is success, 401 means server is up but needs auth
                    if (responseCode == 200) {
                        SwingUtilities.invokeLater {
                            statusLabel.text = "● Backend: Online"
                            statusLabel.foreground = java.awt.Color.GREEN
                        }
                    } else if (responseCode == 401) {
                        SwingUtilities.invokeLater {
                            statusLabel.text = "● Backend: Online (Auth Required)"
                            statusLabel.foreground = java.awt.Color.ORANGE
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
                                val gson = com.google.gson.Gson()
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

    // Dashboard Panel
    class SupremeAIDashboardPanel(private val project: Project) {
        private val panel = JPanel(BorderLayout())
        private val learningCountLabel = JLabel("0")
        private val editCountLabel = JLabel("0")
        private val errorCountLabel = JLabel("0")
        private val feedbackCountLabel = JLabel("0")
        private val recentActivityArea = JTextArea()
        private val statusLabel = JLabel("● Loading...")
        private val refreshTimeLabel = JLabel("")

        init {
            setupUI()
            refresh()
            // Auto-refresh every 30 seconds
            javax.swing.Timer(30000) {
                SwingUtilities.invokeLater { refresh() }
            }.start()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(15, 15, 15, 15)
            panel.layout = BorderLayout()

            // Header
            val header = JPanel(BorderLayout())
            val titlePanel = JPanel()
            titlePanel.layout = BoxLayout(titlePanel, BoxLayout.Y_AXIS)
            titlePanel.add(JLabel("SupremeAI Dashboard").apply { font = font.deriveFont(java.awt.Font.BOLD, 18f) })
            titlePanel.add(Box.createVerticalStrut(5))
            titlePanel.add(JLabel("Real-time Learning Analytics").apply { foreground = Color.GRAY })
            header.add(titlePanel, BorderLayout.WEST)
            
            val headerRight = JPanel(FlowLayout(FlowLayout.RIGHT))
            statusLabel.font = statusLabel.font.deriveFont(java.awt.Font.BOLD, 12f)
            headerRight.add(statusLabel)
            headerRight.add(Box.createHorizontalStrut(10))
            headerRight.add(refreshTimeLabel)
            header.add(headerRight, BorderLayout.EAST)
            panel.add(header, BorderLayout.NORTH)

            // Stats Grid
            val statsPanel = JPanel(GridLayout(2, 2, 10, 10))
            statsPanel.border = EmptyBorder(10, 0, 10, 0)

            statsPanel.add(createStatCard("Patterns Learned", learningCountLabel, Color(76, 175, 80)))
            statsPanel.add(createStatCard("Code Edits", editCountLabel, Color(33, 150, 243)))
            statsPanel.add(createStatCard("Errors Reported", errorCountLabel, Color(244, 67, 54)))
            statsPanel.add(createStatCard("Feedback Given", feedbackCountLabel, Color(156, 39, 176)))

            panel.add(statsPanel, BorderLayout.CENTER)

            // Recent Activity
            val activityPanel = JPanel(BorderLayout())
            activityPanel.border = EmptyBorder(10, 0, 0, 0)
            activityPanel.add(JLabel("Recent Activity").apply { 
                font = font.deriveFont(java.awt.Font.BOLD, 14f)
                border = EmptyBorder(0, 0, 5, 0)
            }, BorderLayout.NORTH)

            recentActivityArea.isEditable = false
            recentActivityArea.font = Font("Monospaced", Font.PLAIN, 11)
            recentActivityArea.lineWrap = true
            recentActivityArea.wrapStyleWord = true
            activityPanel.add(JBScrollPane(recentActivityArea), BorderLayout.CENTER)

            panel.add(activityPanel, BorderLayout.SOUTH)
        }

        private fun createStatCard(title: String, valueLabel: JLabel, color: Color): JPanel {
            val card = JPanel(BorderLayout())
            card.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(224, 224, 224)),
                EmptyBorder(15, 15, 15, 15)
            )
            card.background = Color.WHITE

            val titleLabel = JLabel(title)
            titleLabel.foreground = Color.GRAY
            titleLabel.font = titleLabel.font.deriveFont(11f)
            card.add(titleLabel, BorderLayout.NORTH)

            valueLabel.font = valueLabel.font.deriveFont(java.awt.Font.BOLD, 28f)
            valueLabel.foreground = color
            valueLabel.horizontalAlignment = SwingConstants.RIGHT
            card.add(valueLabel, BorderLayout.CENTER)

            return card
        }

        fun refresh() {
            thread {
                try {
                    val settings = SupremeAISettings.getInstance()
                    val endpoint = settings.apiEndpoint.takeIf { it.isNotBlank() }
                        ?: "https://supremeai-a.web.app"

                    val url = URI("$endpoint/api/knowledge/stats").toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000

                    val responseCode = conn.responseCode
                    if (responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val gson = com.google.gson.Gson()
                        val json = gson.fromJson(response, com.google.gson.JsonObject::class.java)

                        SwingUtilities.invokeLater {
                            learningCountLabel.text = json.getAsJsonPrimitive("learningCount")?.asString ?: "0"
                            editCountLabel.text = json.getAsJsonPrimitive("editCount")?.asString ?: "0"
                            errorCountLabel.text = json.getAsJsonPrimitive("errorCount")?.asString ?: "0"
                            feedbackCountLabel.text = json.getAsJsonPrimitive("feedbackCount")?.asString ?: "0"

                            // Update recent activity
                            val activities = json.getAsJsonArray("recentActivity")
                            val activityText = StringBuilder()
                            if (activities != null) {
                                for (i in 0 until minOf(activities.size(), 10)) {
                                    val act = activities[i].asJsonObject
                                    val time = act.getAsJsonPrimitive("timestamp")?.asString ?: ""
                                    val msg = act.getAsJsonPrimitive("message")?.asString ?: ""
                                    val type = act.getAsJsonPrimitive("type")?.asString ?: ""
                                    val icon = when(type) {
                                        "CODE_EDIT" -> "✏️"
                                        "ERROR_REPORT" -> "❌"
                                        "SUGGESTION_FEEDBACK" -> "👍"
                                        else -> "ℹ️"
                                    }
                                    activityText.append("$icon $msg ($time)\n")
                                }
                            }
                            if (activityText.isEmpty()) {
                                activityText.append("No recent activity\n")
                            }
                            recentActivityArea.text = activityText.toString()

                            statusLabel.text = "● Backend: Online"
                            statusLabel.foreground = Color.GREEN
                            refreshTimeLabel.text = "Updated: ${SimpleDateFormat("HH:mm:ss").format(Date())}"
                        }
                    } else {
                        SwingUtilities.invokeLater {
                            statusLabel.text = "● Backend: Error"
                            statusLabel.foreground = Color.ORANGE
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = "● Backend: Offline"
                        statusLabel.foreground = Color.RED
                        recentActivityArea.text = "Cannot connect to backend\n${e.message}"
                    }
                }
            }
        }

        fun getContent(): JPanel = panel
    }

    // Activity Panel
    class SupremeAIActivityPanel {
        private val panel = JPanel(BorderLayout())
        private val tableModel = DefaultTableModel(arrayOf("Time", "Type", "Message"), 0)
        private val table = JTable(tableModel)
        private val statusLabel = JLabel("Loading...")
        private val clearBtn = JButton("Clear History")

        init {
            setupUI()
            refresh()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(15, 15, 15, 15)

            // Toolbar
            val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))
            toolbar.add(JLabel("Activity Log").apply { font = font.deriveFont(java.awt.Font.BOLD, 14f) })
            toolbar.add(Box.createHorizontalStrut(10))
            clearBtn.addActionListener { clearHistory() }
            toolbar.add(clearBtn)
            toolbar.add(Box.createHorizontalGlue())
            toolbar.add(statusLabel)
            panel.add(toolbar, BorderLayout.NORTH)

            // Table
            table.fillsViewportHeight = true
            table.rowHeight = 22
            tableModel.addRow(arrayOf("Just now", "INFO", "Activity log initialized"))
            panel.add(JBScrollPane(table), BorderLayout.CENTER)
        }

        private fun clearHistory() {
            val settings = SupremeAISettings.getInstance()
            thread {
                try {
                    val endpoint = settings.apiEndpoint.takeIf { it.isNotBlank() }
                        ?: "https://supremeai-a.web.app"
                    val url = URI("$endpoint/api/knowledge/clear").toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.connectTimeout = 5000

                    if (conn.responseCode == 200) {
                        SwingUtilities.invokeLater {
                            tableModel.rowCount = 0
                            tableModel.addRow(arrayOf(SimpleDateFormat("HH:mm:ss").format(Date()), "INFO", "History cleared"))
                            statusLabel.text = "History cleared"
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = "Clear failed: ${e.message}"
                    }
                }
            }
        }

        fun refresh() {
            thread {
                try {
                    val settings = SupremeAISettings.getInstance()
                    val endpoint = settings.apiEndpoint.takeIf { it.isNotBlank() }
                        ?: "https://supremeai-a.web.app"

                    val url = URI("$endpoint/api/knowledge/history").toURL()
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000

                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val gson = com.google.gson.Gson()
                        val json = gson.fromJson(response, com.google.gson.JsonObject::class.java)
                        val items = json.getAsJsonArray("items")

                        SwingUtilities.invokeLater {
                            tableModel.rowCount = 0
                            if (items != null) {
                                for (i in 0 until items.size()) {
                                    val item = items[i].asJsonObject
                                    val time = item.getAsJsonPrimitive("timestamp")?.asString?.substring(11, 19) ?: ""
                                    val type = item.getAsJsonPrimitive("type")?.asString ?: ""
                                    val msg = item.getAsJsonPrimitive("message")?.asString ?: ""
                                    tableModel.addRow(arrayOf(time, type, msg))
                                }
                            }
                            statusLabel.text = "Loaded ${tableModel.rowCount} items"
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = "Load failed: ${e.message}"
                    }
                }
            }
        }

        fun getContent(): JPanel = panel
    }
}
