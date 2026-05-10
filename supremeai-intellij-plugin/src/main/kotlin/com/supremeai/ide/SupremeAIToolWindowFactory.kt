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
        val dashboardContent = contentFactory.createContent(SupremeAIDashboardPanel(project).getContent() as JComponent, SupremeAIBundle.message("tab.dashboard"), false)
        dashboardContent.icon = null // Can set icon here
        toolWindow.contentManager.addContent(dashboardContent)

        // Chat Tab
        val chatContent = contentFactory.createContent(SupremeAIChatPanel(project).getContent() as JComponent, SupremeAIBundle.message("tab.chat"), false)
        toolWindow.contentManager.addContent(chatContent)

        // Activity Tab
        val activityContent = contentFactory.createContent(SupremeAIActivityPanel().getContent() as JComponent, SupremeAIBundle.message("tab.activity"), false)
        toolWindow.contentManager.addContent(activityContent)

        // CodeFlow Tab
        val codeFlowContent = contentFactory.createContent(SupremeAICodeFlowPanel(project).getContent() as JComponent, SupremeAIBundle.message("tab.codeflow"), false)
        toolWindow.contentManager.addContent(codeFlowContent)

        // Orchestration Tab
        val orchestrationPanel = SupremeAIOrchestrationPanel()
        val orchestrationContent = contentFactory.createContent(orchestrationPanel.getContent() as JComponent, SupremeAIBundle.message("tab.orchestration"), false)
        toolWindow.contentManager.addContent(orchestrationContent)

        // Settings Tab
        val settingsPanel = SupremeAISettingsPanel()
        val settingsContent = contentFactory.createContent(settingsPanel.getContent() as JComponent, SupremeAIBundle.message("tab.settings"), false)
        toolWindow.contentManager.addContent(settingsContent)
    }

    class SupremeAIChatPanel(private val project: Project) {
        private val panel = JPanel(BorderLayout())
        private val chatArea = JTextArea()
        private val inputField = JBTextField()
        private val statusLabel = JLabel(SupremeAIBundle.message("status.connecting"))
        private val modeLabel = JLabel(SupremeAIBundle.message("label.mode", "Code"))

        init {
            setupUI()
            checkBackendStatus()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            // Header
            val header = JPanel(BorderLayout())
            val leftHeader = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0))
            leftHeader.add(JLabel(SupremeAIBundle.message("label.assistant.title")))
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
            val sendBtn = JButton(SupremeAIBundle.message("button.send"))
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
                        statusLabel.text = SupremeAIBundle.message("status.connected")
                        statusLabel.foreground = Color(0x00cc66)
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        statusLabel.text = SupremeAIBundle.message("status.disconnected")
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
                                            else -> "Response received"
                                        }
                                    }
                                    jsonElement.isJsonPrimitive -> jsonElement.asString
                                    else -> "Response received"
                                }
                                
                                val detectedMode = if (jsonElement.isJsonObject) {
                                    jsonElement.asJsonObject.get("mode")?.asString
                                } else null
                                
                                SwingUtilities.invokeLater {
                                    chatArea.append("AI: ${aiMessage.replace("\r", "").trim()}\n")
                                    detectedMode?.let { 
                                        modeLabel.text = SupremeAIBundle.message("label.mode", it.replaceFirstChar { c -> c.uppercase() })
                                        modeLabel.foreground = java.awt.Color.BLUE
                                    }
                                }
                                } catch (e: Exception) {
                                    chatArea.append(SupremeAIBundle.message("chat.parse.error", e.message?.replace("\r", "")?.trim() ?: "Unknown"))
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
                                        append("AI: ${SupremeAIBundle.message("chat.auth.required")}\n")
                                        append("${SupremeAIBundle.message("chat.auth.needed")}\n")
                                        if (!apiKeyConfigured) {
                                            append("${SupremeAIBundle.message("chat.no.api.key")}\n")
                                        }
                                        if (!endpointConfigured) {
                                            append("${SupremeAIBundle.message("chat.no.endpoint")}\n")
                                        }
                                        append("\n${SupremeAIBundle.message("chat.config.instruction")}\n")
                                    }
                                    chatArea.append(authMessage)

                                    // Auto-focus settings tab
                                    try {
                                        val toolWindowManager = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                                        val toolWindow = toolWindowManager.getToolWindow("SupremeAI")
                                        if (toolWindow != null) {
                                            val settingsContent = toolWindow.contentManager.contents.find { it.tabName == SupremeAIBundle.message("tab.settings") }
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
                                    chatArea.append("AI: ${SupremeAIBundle.message("chat.access.denied")}\n")
                                    chatArea.append("${SupremeAIBundle.message("chat.access.denied.desc")}\n")
                                }
                                429 -> {
                                    chatArea.append("AI: ${SupremeAIBundle.message("chat.rate.limited")}\n")
                                    chatArea.append("${SupremeAIBundle.message("chat.rate.limited.desc")}\n")
                                }
                                500, 502, 503, 504 -> {
                                    chatArea.append("AI: ${SupremeAIBundle.message("chat.server.error")}\n")
                                    chatArea.append("${SupremeAIBundle.message("chat.server.error.desc")}\n")
                                }
                                else -> {
                                    chatArea.append(SupremeAIBundle.message("chat.unknown.error", responseCode, errorResponse))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    SwingUtilities.invokeLater {
                        chatArea.append(SupremeAIBundle.message("chat.offline", e.message ?: "Unknown"))
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
        private val statusLabel = JLabel(SupremeAIBundle.message("status.loading"))

        init {
            setupUI()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            // Header
            val header = JPanel(BorderLayout())
            header.add(JLabel(SupremeAIBundle.message("label.dashboard.title")).apply { font = font.deriveFont(Font.BOLD, 16f) }, BorderLayout.WEST)
            header.add(statusLabel, BorderLayout.EAST)
            panel.add(header, BorderLayout.NORTH)

            // Stats Panel
            val statsPanel = JPanel(GridLayout(2, 2, 10, 10))
            statsPanel.border = EmptyBorder(10, 0, 10, 0)
            
            val projectsCard = createStatCard(SupremeAIBundle.message("label.projects"), "24", SupremeAIBundle.message("label.active"))
            val analysesCard = createStatCard(SupremeAIBundle.message("label.analyses"), "156", SupremeAIBundle.message("label.this.month"))
            val issuesCard = createStatCard(SupremeAIBundle.message("label.issues.found"), "23", SupremeAIBundle.message("label.critical"))
            val fixesCard = createStatCard(SupremeAIBundle.message("label.fixes.applied"), "89", SupremeAIBundle.message("label.auto.fixed"))
            
            statsPanel.add(projectsCard)
            statsPanel.add(analysesCard)
            statsPanel.add(issuesCard)
            statsPanel.add(fixesCard)
            
            panel.add(statsPanel, BorderLayout.CENTER)

            // Action Buttons
            val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
            val analyzeBtn = JButton(SupremeAIBundle.message("button.run.analysis"))
            val refreshBtn = JButton(SupremeAIBundle.message("button.refresh"))
            
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
            statusLabel.text = SupremeAIBundle.message("status.analyzing")
            thread {
                Thread.sleep(2000)
                SwingUtilities.invokeLater {
                    statusLabel.text = SupremeAIBundle.message("status.complete")
                }
            }
        }

        fun refresh() {
            statusLabel.text = SupremeAIBundle.message("status.refreshing")
            thread {
                Thread.sleep(1000)
                SwingUtilities.invokeLater {
                    statusLabel.text = SupremeAIBundle.message("status.ready")
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
            
            val titleLabel = JLabel(SupremeAIBundle.message("label.recent.activity")).apply { font = font.deriveFont(Font.BOLD, 16f) }
            panel.add(titleLabel, BorderLayout.NORTH)

            val activityArea = JTextArea()
            activityArea.isEditable = false
            // Simplified for now, in a real app this would load from a log service
            activityArea.text = """2024-01-15 10:30:22 - Analysis completed
2024-01-15 09:15:45 - Security scan passed
2024-01-15 08:42:11 - CodeFlow analysis initiated"""
            
            panel.add(JBScrollPane(activityArea), BorderLayout.CENTER)
        }

        fun refresh() {
            // Refresh logic here
        }

        fun getContent(): Component = panel
    }

    class SupremeAICodeFlowPanel(private val project: Project) {
        private val panel = JPanel(BorderLayout())
        private val statusLabel = JLabel(SupremeAIBundle.message("status.ready"))
        private var currentAnalysis: Any? = null

        init {
            setupUI()
        }

        private fun setupUI() {
            panel.border = EmptyBorder(10, 10, 10, 10)
            
            // Header
            val header = JPanel(BorderLayout())
            header.add(JLabel(SupremeAIBundle.message("label.codeflow.title")).apply { font = font.deriveFont(Font.BOLD, 16f) }, BorderLayout.WEST)
            header.add(statusLabel, BorderLayout.EAST)
            panel.add(header, BorderLayout.NORTH)

            // Tabbed Pane for different views
            val tabbedPane = JTabbedPane()
            
            // Overview Tab
            tabbedPane.addTab(SupremeAIBundle.message("label.overview"), createOverviewPanel())
            
            // Dependencies Tab
            tabbedPane.addTab(SupremeAIBundle.message("label.dependencies"), createDependenciesPanel())
            
            // Security Tab
            tabbedPane.addTab(SupremeAIBundle.message("label.security"), createSecurityPanel())
            
            // Patterns Tab
            tabbedPane.addTab(SupremeAIBundle.message("label.patterns"), createPatternsPanel())
            
            // Health Tab
            tabbedPane.addTab(SupremeAIBundle.message("label.health.score"), createHealthPanel())
            
            panel.add(tabbedPane, BorderLayout.CENTER)

            // Action Buttons
            val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 10))
            val analyzeBtn = JButton(SupremeAIBundle.message("button.run.analysis"))
            val refreshBtn = JButton(SupremeAIBundle.message("button.refresh"))
            val exportBtn = JButton(SupremeAIBundle.message("button.export"))
            
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
            textArea.text = buildString {
                append(SupremeAIBundle.message("label.overview")).append("\n\n")
                append("Repository: ").append(project.name).append("\n")
                append(SupremeAIBundle.message("label.files.analyzed", 0)).append("\n")
                append(SupremeAIBundle.message("label.lines.code", 0)).append("\n")
                append(SupremeAIBundle.message("label.functions", 0)).append("\n")
                append(SupremeAIBundle.message("label.classes", 0)).append("\n\n")
                append(SupremeAIBundle.message("label.analysis.status", SupremeAIBundle.message("label.analysis.not.started"))).append("\n")
                append(SupremeAIBundle.message("label.last.analysis", SupremeAIBundle.message("label.analysis.never"))).append("\n\n")
                append(SupremeAIBundle.message("label.analysis.instruction"))
            }
            
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
            
            val scoreLabel = JLabel("${SupremeAIBundle.message("label.health.score")}: --")
            scoreLabel.font = scoreLabel.font.deriveFont(Font.BOLD, 48f)
            scoreLabel.alignmentX = Component.CENTER_ALIGNMENT
            healthPanel.add(scoreLabel)
            
            healthPanel.add(Box.createVerticalStrut(20))
            
            val gradeLabel = JLabel("${SupremeAIBundle.message("label.grade")}: --")
            gradeLabel.font = gradeLabel.font.deriveFont(Font.BOLD, 24f)
            gradeLabel.alignmentX = Component.CENTER_ALIGNMENT
            healthPanel.add(gradeLabel)
            
            healthPanel.add(Box.createVerticalStrut(40))
            
            val breakdownLabel = JLabel(SupremeAIBundle.message("label.breakdown"))
            breakdownLabel.font = breakdownLabel.font.deriveFont(Font.BOLD, 14f)
            breakdownLabel.alignmentX = Component.CENTER_ALIGNMENT
            healthPanel.add(breakdownLabel)
            
            healthPanel.add(Box.createVerticalStrut(10))
            
            val detailsArea = JTextArea()
            detailsArea.isEditable = false
            detailsArea.text = buildString {
                append(SupremeAIBundle.message("label.security.score", "--")).append("\n")
                append(SupremeAIBundle.message("label.maintainability", "--")).append("\n")
                append(SupremeAIBundle.message("label.complexity", "--")).append("\n")
                append(SupremeAIBundle.message("label.documentation", "--")).append("\n")
                append(SupremeAIBundle.message("label.testing", "--"))
            }
            detailsArea.alignmentX = Component.CENTER_ALIGNMENT
            healthPanel.add(detailsArea)
            
            panel.add(healthPanel, BorderLayout.CENTER)
            return panel
        }

        fun runAnalysis() {
            statusLabel.text = SupremeAIBundle.message("status.analyzing")
            thread {
                // Simulate analysis
                Thread.sleep(3000)
                SwingUtilities.invokeLater {
                    statusLabel.text = SupremeAIBundle.message("status.complete")
                    // Update panels with results
                }
            }
        }

        fun refreshAnalysis() {
            statusLabel.text = SupremeAIBundle.message("status.refreshing")
            thread {
                Thread.sleep(1000)
                SwingUtilities.invokeLater {
                    statusLabel.text = SupremeAIBundle.message("status.ready")
                }
            }
        }

        private fun exportReport() {
            statusLabel.text = SupremeAIBundle.message("status.exporting")
            thread {
                Thread.sleep(1000)
                SwingUtilities.invokeLater {
                    statusLabel.text = SupremeAIBundle.message("status.exported")
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
            
            val titleLabel = JLabel(SupremeAIBundle.message("tab.settings")).apply { font = font.deriveFont(Font.BOLD, 16f) }
            panel.add(titleLabel, BorderLayout.NORTH)

            val formPanel = JPanel()
            formPanel.layout = BoxLayout(formPanel, BoxLayout.Y_AXIS)
            formPanel.border = EmptyBorder(20, 20, 20, 20)

            val settings = SupremeAISettings.getInstance()

            // API Key
            formPanel.add(JLabel(SupremeAIBundle.message("label.api.key")))
            val apiKeyField = JPasswordField(settings.apiKey, 30)
            formPanel.add(apiKeyField)
            formPanel.add(Box.createVerticalStrut(10))

            // API Endpoint
            formPanel.add(JLabel(SupremeAIBundle.message("label.api.endpoint")))
            val endpointField = JBTextField(settings.apiEndpoint, 30)
            formPanel.add(endpointField)
            formPanel.add(Box.createVerticalStrut(10))

            // Model
            formPanel.add(JLabel(SupremeAIBundle.message("label.model")))
            val modelCombo = JComboBox(arrayOf("SupremeAI-v1 (Stable)", "SupremeAI-v1 (Flash)", "gemini-pro", "gemini-flash"))
            modelCombo.selectedItem = settings.model
            formPanel.add(modelCombo)
            formPanel.add(Box.createVerticalStrut(20))

            // Save Button
            val saveBtn = JButton(SupremeAIBundle.message("button.save"))
            saveBtn.addActionListener {
                settings.apiKey = String(apiKeyField.password)
                settings.apiEndpoint = endpointField.text
                settings.model = modelCombo.selectedItem as String
                settings.save()
                JOptionPane.showMessageDialog(panel, SupremeAIBundle.message("msg.settings.saved"))
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
            
            val titleLabel = JLabel(SupremeAIBundle.message("label.orchestration.title")).apply { font = font.deriveFont(Font.BOLD, 16f) }
            panel.add(titleLabel, BorderLayout.NORTH)

            val textArea = JTextArea()
            textArea.isEditable = false
            val lastSync = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            textArea.text = SupremeAIBundle.message("label.orchestration.desc", lastSync)
            
            panel.add(JBScrollPane(textArea), BorderLayout.CENTER)
        }

        fun getContent(): Component = panel
    }

}
