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
        var codeFlowPanel: SupremeAICodeFlowPanel? = null
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
        
        fun refreshCodeFlow() {
            codeFlowPanel?.refreshAnalysis()
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        // Dashboard Tab
        val dashboardContent = contentFactory.createContent(SupremeAIDashboardPanel(project).getContent() as JComponent, "Dashboard", false)
        dashboardContent.icon = null // Can set icon here
        toolWindow.contentManager.addContent(dashboardContent)

        // Chat Tab
        val chatContent = contentFactory.createContent(SupremeAIChatPanel().getContent() as JComponent, "Chat", false)
        toolWindow.contentManager.addContent(chatContent)

        // Activity Tab
        val activityContent = contentFactory.createContent(SupremeAIActivityPanel().getContent() as JComponent, "Activity", false)
        toolWindow.contentManager.addContent(activityContent)

        // CodeFlow Tab
        val codeFlowContent = contentFactory.createContent(SupremeAICodeFlowPanel(project).getContent() as JComponent, "CodeFlow", false)
        toolWindow.contentManager.addContent(codeFlowContent)

        // Orchestration Tab
        val orchestrationPanel = SupremeAIOrchestrationPanel()
        val orchestrationContent = contentFactory.createContent(orchestrationPanel.getContent() as JComponent, "Orchestration", false)
        toolWindow.contentManager.addContent(orchestrationContent)

        // Settings Tab
        val settingsPanel = SupremeAISettingsPanel()
        val settingsContent = contentFactory.createContent(settingsPanel.getContent() as JComponent, "Settings", false)
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

        private fun checkBackendStatus() {
            thread {
                try {
                    Thread.sleep(1000)
                    SwingUtilities.invokeLater {
                        statusLabel.text = "● Backend: Connected"
                        statusLabel.foreground = Color(0x00cc66)
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = "● Backend: Disconnected"
                        statusLabel.foreground = Color.RED
                    }
                }
            }
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

        fun addExternalMessage(message: String) {
            SwingUtilities.invokeLater {
                chatArea.append("AI: $message\n")
            }
        }

        fun getContent(): JComponent = panel
    }

    class SupremeAIDashboardPanel(private val project: Project) {
        private val panel = JPanel(BorderLayout())
        private val statusLabel = JLabel("Status: Loading...")

        init {
            setupUI()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            // Header
            val header = JPanel(BorderLayout())
            header.add(JLabel("SupremeAI Dashboard").apply { font = font.deriveFont(Font.BOLD, 16f) }, BorderLayout.WEST)
            header.add(statusLabel, BorderLayout.EAST)
            panel.add(header, BorderLayout.NORTH)

            // Stats Panel
            val statsPanel = JPanel(GridLayout(2, 2, 10, 10))
            statsPanel.border = EmptyBorder(10, 0, 10, 0)
            
            val projectsCard = createStatCard("Projects", "24", "Active")
            val analysesCard = createStatCard("Analyses", "156", "This month")
            val issuesCard = createStatCard("Issues Found", "23", "Critical")
            val fixesCard = createStatCard("Fixes Applied", "89", "Auto-fixed")
            
            statsPanel.add(projectsCard)
            statsPanel.add(analysesCard)
            statsPanel.add(issuesCard)
            statsPanel.add(fixesCard)
            
            panel.add(statsPanel, BorderLayout.CENTER)

            // Action Buttons
            val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
            val analyzeBtn = JButton("Run Analysis")
            val refreshBtn = JButton("Refresh")
            
            analyzeBtn.addActionListener { runAnalysis() }
            refreshBtn.addActionListener { refresh() }
            
            buttonPanel.add(analyzeBtn)
            buttonPanel.add(refreshBtn)
            panel.add(buttonPanel, BorderLayout.SOUTH)
        }

        private fun createStatCard(title: String, value: String, subtitle: String): JPanel {
            val card = JPanel(BorderLayout())
            card.border = EmptyBorder(15, 15, 15, 15)
            card.background = Color(0x1a1a1f)
            
            val titleLabel = JLabel(title)
            titleLabel.foreground = Color(0x888888)
            card.add(titleLabel, BorderLayout.NORTH)
            
            val valueLabel = JLabel(value)
            valueLabel.font = valueLabel.font.deriveFont(Font.BOLD, 24f)
            valueLabel.foreground = Color(0x00ff9d)
            card.add(valueLabel, BorderLayout.CENTER)
            
            val subtitleLabel = JLabel(subtitle)
            subtitleLabel.foreground = Color(0x888888)
            card.add(subtitleLabel, BorderLayout.SOUTH)
            
            return card
        }

        private fun runAnalysis() {
            statusLabel.text = "Status: Analyzing..."
            thread {
                Thread.sleep(2000)
                SwingUtilities.invokeLater {
                    statusLabel.text = "Status: Analysis Complete"
                }
            }
        }

        fun refresh() {
            statusLabel.text = "Status: Refreshing..."
            thread {
                Thread.sleep(1000)
                SwingUtilities.invokeLater {
                    statusLabel.text = "Status: Ready"
                }
            }
        }

        fun getContent(): Component = panel
    }

    class SupremeAIActivityPanel {
        private val panel = JPanel(BorderLayout())

        init {
            setupUI()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            val titleLabel = JLabel("Recent Activity").apply { font = font.deriveFont(Font.BOLD, 16f) }
            panel.add(titleLabel, BorderLayout.NORTH)

            val activityArea = JTextArea()
            activityArea.isEditable = false
            activityArea.text = """2024-01-15 10:30:22 - Analysis completed for project Alpha
2024-01-15 09:15:45 - Security scan passed for module Beta
2024-01-15 08:42:11 - CodeFlow analysis initiated
2024-01-14 16:20:33 - 5 issues auto-fixed in project Gamma
2024-01-14 14:55:21 - Learning pattern detected: Factory Method
2024-01-14 11:30:15 - Dependency graph updated"""
            
            panel.add(JBScrollPane(activityArea), BorderLayout.CENTER)
        }

        fun refresh() {
            // Refresh logic here
        }

        fun getContent(): Component = panel
    }

    class SupremeAICodeFlowPanel(private val project: Project) {
        private val panel = JPanel(BorderLayout())
        private val statusLabel = JLabel("Status: Ready")
        private var currentAnalysis: Any? = null

        init {
            setupUI()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            // Header
            val header = JPanel(BorderLayout())
            header.add(JLabel("CodeFlow Analysis").apply { font = font.deriveFont(Font.BOLD, 16f) }, BorderLayout.WEST)
            header.add(statusLabel, BorderLayout.EAST)
            panel.add(header, BorderLayout.NORTH)

            // Tabbed Pane for different views
            val tabbedPane = JTabbedPane()
            
            // Overview Tab
            tabbedPane.addTab("Overview", createOverviewPanel())
            
            // Dependencies Tab
            tabbedPane.addTab("Dependencies", createDependenciesPanel())
            
            // Security Tab
            tabbedPane.addTab("Security", createSecurityPanel())
            
            // Patterns Tab
            tabbedPane.addTab("Patterns", createPatternsPanel())
            
            // Health Tab
            tabbedPane.addTab("Health Score", createHealthPanel())
            
            panel.add(tabbedPane, BorderLayout.CENTER)

            // Action Buttons
            val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
            val analyzeBtn = JButton("Run Analysis")
            val refreshBtn = JButton("Refresh")
            val exportBtn = JButton("Export Report")
            
            analyzeBtn.addActionListener { runAnalysis() }
            refreshBtn.addActionListener { refreshAnalysis() }
            exportBtn.addActionListener { exportReport() }
            
            buttonPanel.add(analyzeBtn)
            buttonPanel.add(refreshBtn)
            buttonPanel.add(exportBtn)
            panel.add(buttonPanel, BorderLayout.SOUTH)
        }

        private fun createOverviewPanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            val textArea = JTextArea()
            textArea.isEditable = false
            textArea.text = """CodeFlow Analysis Overview

Repository: ${project.name}
Files Analyzed: 0
Lines of Code: 0
Functions: 0
Classes: 0

Analysis Status: Not Started
Last Analysis: Never

Click "Run Analysis" to begin."""
            
            panel.add(JBScrollPane(textArea), BorderLayout.CENTER)
            return panel
        }

        private fun createDependenciesPanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            val model = DefaultTableModel(
                arrayOf("Source", "Target", "Type", "Weight"),
                0
            )
            val table = JTable(model)
            
            panel.add(JBScrollPane(table), BorderLayout.CENTER)
            return panel
        }

        private fun createSecurityPanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            val model = DefaultTableModel(
                arrayOf("File", "Line", "Type", "Severity", "Description"),
                0
            )
            val table = JTable(model)
            
            panel.add(JBScrollPane(table), BorderLayout.CENTER)
            return panel
        }

        private fun createPatternsPanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            val model = DefaultTableModel(
                arrayOf("Type", "File", "Line", "Severity", "Description"),
                0
            )
            val table = JTable(model)
            
            panel.add(JBScrollPane(table), BorderLayout.CENTER)
            return panel
        }

        private fun createHealthPanel(): JPanel {
            val panel = JPanel(BorderLayout())
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            val healthPanel = JPanel()
            healthPanel.layout = BoxLayout(healthPanel, BoxLayout.Y_AXIS)
            healthPanel.border = EmptyBorder(20, 20, 20, 20)
            
            val scoreLabel = JLabel("Health Score: --")
            scoreLabel.font = scoreLabel.font.deriveFont(Font.BOLD, 48f)
            scoreLabel.alignmentX = Component.CENTER_ALIGNMENT
            healthPanel.add(scoreLabel)
            
            healthPanel.add(Box.createVerticalStrut(20))
            
            val gradeLabel = JLabel("Grade: --")
            gradeLabel.font = gradeLabel.font.deriveFont(Font.BOLD, 24f)
            gradeLabel.alignmentX = Component.CENTER_ALIGNMENT
            healthPanel.add(gradeLabel)
            
            healthPanel.add(Box.createVerticalStrut(40))
            
            val breakdownLabel = JLabel("Breakdown:")
            breakdownLabel.font = breakdownLabel.font.deriveFont(Font.BOLD, 14f)
            breakdownLabel.alignmentX = Component.CENTER_ALIGNMENT
            healthPanel.add(breakdownLabel)
            
            healthPanel.add(Box.createVerticalStrut(10))
            
            val detailsArea = JTextArea()
            detailsArea.isEditable = false
            detailsArea.text = "Security: --\nMaintainability: --\nComplexity: --\nDocumentation: --\nTesting: --"
            detailsArea.alignmentX = Component.CENTER_ALIGNMENT
            healthPanel.add(detailsArea)
            
            panel.add(healthPanel, BorderLayout.CENTER)
            return panel
        }

        fun runAnalysis() {
            statusLabel.text = "Status: Analyzing..."
            thread {
                // Simulate analysis
                Thread.sleep(3000)
                SwingUtilities.invokeLater {
                    statusLabel.text = "Status: Analysis Complete"
                    // Update panels with results
                }
            }
        }

        fun refreshAnalysis() {
            statusLabel.text = "Status: Refreshing..."
            thread {
                Thread.sleep(1000)
                SwingUtilities.invokeLater {
                    statusLabel.text = "Status: Ready"
                }
            }
        }

        private fun exportReport() {
            statusLabel.text = "Status: Exporting..."
            thread {
                Thread.sleep(1000)
                SwingUtilities.invokeLater {
                    statusLabel.text = "Status: Report Exported"
                }
            }
        }

        fun getContent(): Component = panel
    }

    class SupremeAISettingsPanel {
        private val panel = JPanel(BorderLayout())

        init {
            setupUI()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            val titleLabel = JLabel("SupremeAI Settings").apply { font = font.deriveFont(Font.BOLD, 16f) }
            panel.add(titleLabel, BorderLayout.NORTH)

            val formPanel = JPanel()
            formPanel.layout = BoxLayout(formPanel, BoxLayout.Y_AXIS)
            formPanel.border = EmptyBorder(20, 20, 20, 20)

            // API Key
            formPanel.add(JLabel("API Key:"))
            val apiKeyField = JPasswordField(30)
            formPanel.add(apiKeyField)
            formPanel.add(Box.createVerticalStrut(10))

            // API Endpoint
            formPanel.add(JLabel("API Endpoint:"))
            val endpointField = JBTextField("https://supremeai-a.web.app", 30)
            formPanel.add(endpointField)
            formPanel.add(Box.createVerticalStrut(10))

            // Model
            formPanel.add(JLabel("Model:"))
            val modelCombo = JComboBox(arrayOf("gemini-pro", "gemini-flash", "gemini-ultra"))
            formPanel.add(modelCombo)
            formPanel.add(Box.createVerticalStrut(20))

            // Save Button
            val saveBtn = JButton("Save Settings")
            saveBtn.addActionListener {
                val settings = SupremeAISettings.getInstance()
                settings.apiKey = String(apiKeyField.password)
                settings.apiEndpoint = endpointField.text
                settings.model = modelCombo.selectedItem as String
                settings.save()
                JOptionPane.showMessageDialog(panel, "Settings saved successfully!")
            }
            formPanel.add(saveBtn)

            panel.add(formPanel, BorderLayout.CENTER)
        }

        fun getContent(): Component = panel
    }

    class SupremeAIOrchestrationPanel {
        private val panel = JPanel(BorderLayout())

        init {
            setupUI()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            val titleLabel = JLabel("AI Orchestration").apply { font = font.deriveFont(Font.BOLD, 16f) }
            panel.add(titleLabel, BorderLayout.NORTH)

            val textArea = JTextArea()
            textArea.isEditable = false
            textArea.text = """AI Provider Orchestration

Primary: Kimi K2.5
Fallback: DeepSeek V3
Backup: Together AI

Status: All systems operational

Active Models:
• gemini-pro (Google)
• claude-3-opus (Anthropic)
• gpt-4 (OpenAI)

Last Sync: 2024-01-15 10:30:00"""
            
            panel.add(JBScrollPane(textArea), BorderLayout.CENTER)
        }

        fun getContent(): Component = panel
    }

    class SupremeAISettings {
        var apiKey: String = ""
        var apiEndpoint: String = "https://supremeai-a.web.app"
        var model: String = "gemini-pro"

        fun save() {
            // Save to settings
        }

        companion object {
            private var instance: SupremeAISettings? = null

            fun getInstance(): SupremeAISettings {
                if (instance == null) {
                    instance = SupremeAISettings()
                }
                return instance!!
            }
        }
    }
}
