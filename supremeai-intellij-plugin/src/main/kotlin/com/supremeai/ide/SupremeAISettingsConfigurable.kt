package com.supremeai.ide

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.NlsContexts.ConfigurableName
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class SupremeAISettingsConfigurable : Configurable {

    private val apiKeyField = JBPasswordField()
    private val apiEndpointField = JBTextField()
    private val kimoModeField = JBCheckBox("Enable Kimo (Advanced Optimization)")
    private val modelField = JBTextField()
    private val smallModelField = JBTextField()
    private val fullAuthorityField = JBCheckBox("Enable full authority mode (allow all)")
    private val shareModeField = JComboBox(arrayOf("manual", "auto", "disabled"))
    private val externalDirectoryField = JBCheckBox("Allow external directory access")

    private val settings = SupremeAISettings.getInstance()

    init {
        apiKeyField.text = settings.apiKey
        apiEndpointField.text = settings.apiEndpoint
        kimoModeField.isSelected = settings.kimoMode
        modelField.text = settings.model
        smallModelField.text = settings.smallModel
        fullAuthorityField.isSelected = settings.fullAuthority
        shareModeField.selectedItem = settings.shareMode
        externalDirectoryField.isSelected = settings.enableExternalDirectory
    }

    @ConfigurableName
    override fun getDisplayName(): String = "SupremeAI"

    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("API Key (Optional for Public API):", apiKeyField)
            .addLabeledComponent("API Endpoint:", apiEndpointField)
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
               apiEndpointField.text != settings.apiEndpoint ||
               kimoModeField.isSelected != settings.kimoMode ||
               modelField.text != settings.model ||
               smallModelField.text != settings.smallModel ||
               fullAuthorityField.isSelected != settings.fullAuthority ||
               (shareModeField.selectedItem as? String ?: "manual") != settings.shareMode ||
               externalDirectoryField.isSelected != settings.enableExternalDirectory
    }

    override fun apply() {
        settings.apiKey = apiKeyField.password.joinToString("")
        settings.apiEndpoint = apiEndpointField.text
        settings.kimoMode = kimoModeField.isSelected
        settings.model = modelField.text
        settings.smallModel = smallModelField.text
        settings.fullAuthority = fullAuthorityField.isSelected
        settings.shareMode = shareModeField.selectedItem as? String ?: "manual"
        settings.enableExternalDirectory = externalDirectoryField.isSelected
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
        apiEndpointField.text = settings.apiEndpoint
        kimoModeField.isSelected = settings.kimoMode
        modelField.text = settings.model
        smallModelField.text = settings.smallModel
        fullAuthorityField.isSelected = settings.fullAuthority
        shareModeField.selectedItem = settings.shareMode
        externalDirectoryField.isSelected = settings.enableExternalDirectory
    }
}