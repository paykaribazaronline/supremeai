package com.supremeai.ide

import com.intellij.openapi.options.Configurable
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.FormBuilder
import java.awt.Font
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class SupremeAISettingsConfigurable : Configurable {

    private val apiKeyField = JBPasswordField()
    private val userNameLabel = JBLabel("Welcome, Developer").apply {
        font = font.deriveFont(Font.BOLD)
    }
    private val dashboardLink = HyperlinkLabel("Open SupremeAI Cloud Dashboard")
    private val kimoModeField = JBCheckBox("Enable Kimo (Advanced Optimization)")
    
    private val modelOptions = arrayOf("SupremeAI-v1 (Stable)", "SupremeAI-v1 (Flash)", "SupremeAI-v2 (Experimental)")
    private val modelField = JComboBox(modelOptions)
    private val smallModelField = JComboBox(modelOptions)
    
    private val fullAuthorityField = JBCheckBox("Enable full authority mode (allow all)")
    private val shareModeField = JComboBox(arrayOf("manual", "auto", "disabled"))
    private val externalDirectoryField = JBCheckBox("Allow external directory access")

    private val settings = SupremeAISettings.getInstance()

    init {
        apiKeyField.text = settings.apiKey
        dashboardLink.setHyperlinkTarget("https://supremeai-a.web.app")
        kimoModeField.isSelected = settings.kimoMode
        modelField.selectedItem = settings.model
        smallModelField.selectedItem = settings.smallModel
        fullAuthorityField.isSelected = settings.fullAuthority
        shareModeField.selectedItem = settings.shareMode
        externalDirectoryField.isSelected = settings.enableExternalDirectory
        
        // Dynamic User Recognition (Mock for now, can be linked to your Auth Service)
        updateUserIdentity()
    }

    private fun updateUserIdentity() {
        // In a real scenario, this would fetch the name from Firebase/Backend
        // For now, we use a placeholder or system user
        val name = System.getProperty("user.name") ?: "Developer"
        userNameLabel.text = "Welcome, $name"
    }

    override fun getDisplayName(): String = "SupremeAI"

    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addComponent(userNameLabel)
            .addVerticalGap(5)
            .addLabeledComponent("Personal API Key:", apiKeyField)
            .addLabeledComponent("System Status:", dashboardLink)
            .addVerticalGap(10)
            .addComponent(kimoModeField)
            .addLabeledComponent("Primary Model:", modelField)
            .addLabeledComponent("Small Model:", smallModelField)
            .addLabeledComponent("Share Mode:", shareModeField)
            .addComponent(fullAuthorityField)
            .addComponent(externalDirectoryField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        return apiKeyField.password.joinToString("") != settings.apiKey ||
               kimoModeField.isSelected != settings.kimoMode ||
               (modelField.selectedItem as? String ?: "") != settings.model ||
               (smallModelField.selectedItem as? String ?: "") != settings.smallModel ||
               fullAuthorityField.isSelected != settings.fullAuthority ||
               (shareModeField.selectedItem as? String ?: "manual") != settings.shareMode ||
               externalDirectoryField.isSelected != settings.enableExternalDirectory
    }

    override fun apply() {
        settings.apiKey = apiKeyField.password.joinToString("")
        settings.kimoMode = kimoModeField.isSelected
        settings.model = modelField.selectedItem as? String ?: "SupremeAI-v1 (Stable)"
        settings.smallModel = smallModelField.selectedItem as? String ?: "SupremeAI-v1 (Flash)"
        settings.fullAuthority = fullAuthorityField.isSelected
        settings.shareMode = shareModeField.selectedItem as? String ?: "manual"
        settings.enableExternalDirectory = externalDirectoryField.isSelected
        
        settings.apiEndpoint = "https://supremeai-a.web.app"
        
        settings.permissions["read"] = if (settings.fullAuthority) "allow" else settings.permissions["read"] ?: "allow"
        settings.permissions["edit"] = if (settings.fullAuthority) "allow" else settings.permissions["edit"] ?: "ask"
        settings.permissions["bash"] = if (settings.fullAuthority) "allow" else settings.permissions["bash"] ?: "ask"
        settings.permissions["task"] = if (settings.fullAuthority) "allow" else settings.permissions["task"] ?: "allow"
        settings.permissions["websearch"] = if (settings.fullAuthority) "allow" else settings.permissions["websearch"] ?: "allow"
        settings.permissions["external_directory"] =
            if (settings.fullAuthority && externalDirectoryField.isSelected) "allow" else "deny"
    }

    override fun reset() {
        apiKeyField.text = settings.apiKey
        kimoModeField.isSelected = settings.kimoMode
        modelField.selectedItem = settings.model
        smallModelField.selectedItem = settings.smallModel
        fullAuthorityField.isSelected = settings.fullAuthority
        shareModeField.selectedItem = settings.shareMode
        externalDirectoryField.isSelected = settings.enableExternalDirectory
    }
}
