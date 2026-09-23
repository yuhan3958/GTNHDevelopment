package dev.gtnh.intellij.project

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@Service(Service.Level.PROJECT)
@State(name = "GtnhDevelopmentFirstRun", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class GtnhFirstRunState : PersistentStateComponent<GtnhFirstRunState.Data> {
    data class Data(var detectionNotificationShown: Boolean = false)
    private var data = Data()
    override fun getState(): Data = data
    override fun loadState(state: Data) { data = state }
    var shown: Boolean
        get() = data.detectionNotificationShown
        set(value) { data.detectionNotificationShown = value }
}
