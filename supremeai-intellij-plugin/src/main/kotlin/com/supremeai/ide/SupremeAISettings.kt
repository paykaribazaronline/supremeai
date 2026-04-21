package com.supremeai.ide

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "SupremeAISettings",
    storages = [Storage("supremeai.xml")]
)
class SupremeAISettings : PersistentStateComponent<SupremeAISettings> {

    var apiKey: String = ""
    var apiEndpoint: String = "https://supremeai-lhlwyikwlq-uc.a.run.app"
    var kimoMode: Boolean = false
    var model: String = "google/gemini-1.5-pro"
    var smallModel: String = "google/gemini-1.5-flash"
    var fullAuthority: Boolean = false
    var shareMode: String = "manual"
    var enableExternalDirectory: Boolean = false
    var permissions: MutableMap<String, String> = mutableMapOf(
        "read" to "allow",
        "edit" to "ask",
        "bash" to "ask",
        "task" to "allow",
        "websearch" to "allow",
        "external_directory" to "deny"
    )

    override fun getState(): SupremeAISettings = this

    override fun loadState(state: SupremeAISettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): SupremeAISettings {
            return ApplicationManager.getApplication().getService(SupremeAISettings::class.java)
        }
    }
}