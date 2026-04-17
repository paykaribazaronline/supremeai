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
    var apiEndpoint: String = "https://supremeai-565236080752.us-central1.run.app"

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