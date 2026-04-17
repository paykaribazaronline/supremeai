package com.supremeai.ide

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.NlsContexts.ConfigurableName
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class SupremeAISettingsConfigurable : Configurable {

    private val apiKeyField = JBPasswordField()
    private val apiEndpointField = JBTextField()

    private val settings = SupremeAISettings.getInstance()

    init {
        apiKeyField.text = settings.apiKey
        apiEndpointField.text = settings.apiEndpoint
    }

    @ConfigurableName
    override fun getDisplayName(): String = "SupremeAI"

    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("API Key:", apiKeyField)
            .addLabeledComponent("API Endpoint:", apiEndpointField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        return apiKeyField.password.joinToString("") != settings.apiKey ||
               apiEndpointField.text != settings.apiEndpoint
    }

    override fun apply() {
        settings.apiKey = apiKeyField.password.joinToString("")
        settings.apiEndpoint = apiEndpointField.text
    }

    override fun reset() {
        apiKeyField.text = settings.apiKey
        apiEndpointField.text = settings.apiEndpoint
    }
}