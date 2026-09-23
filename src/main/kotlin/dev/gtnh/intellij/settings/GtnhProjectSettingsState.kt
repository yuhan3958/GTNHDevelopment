package dev.gtnh.intellij.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

enum class GtnhDetectionOverride { AUTO, FORCE_ON, FORCE_OFF }

@Service(Service.Level.PROJECT)
@State(name = "GtnhDevelopmentProjectSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class GtnhProjectSettingsState : PersistentStateComponent<GtnhProjectSettingsState.Data> {
    data class Data(var detectionOverride: GtnhDetectionOverride = GtnhDetectionOverride.AUTO)
    private var data = Data()
    override fun getState(): Data = data
    override fun loadState(state: Data) { data = state }
    var detectionOverride: GtnhDetectionOverride
        get() = data.detectionOverride
        set(value) { data.detectionOverride = value }
}
