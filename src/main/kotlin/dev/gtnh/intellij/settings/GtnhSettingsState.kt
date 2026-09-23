package dev.gtnh.intellij.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "GtnhDevelopmentSettings", storages = [Storage("gtnhDevelopment.xml")])
class GtnhSettingsState : PersistentStateComponent<GtnhSettingsState.Data> {
    data class Data(
        var automaticDetection: Boolean = true,
        var gutterIcons: Boolean = true,
        var mixinNavigation: Boolean = true,
        var patchImpact: Boolean = true,
        var patchHeuristics: Boolean = true,
        var environmentDoctor: Boolean = true,
        var prAssistant: Boolean = true,
        var firstRunNotifications: Boolean = true
    )

    private var data = Data()
    override fun getState(): Data = data
    override fun loadState(state: Data) { data = state }

    companion object {
        fun getInstance(): GtnhSettingsState = ApplicationManager.getApplication().getService(GtnhSettingsState::class.java)
    }
}
