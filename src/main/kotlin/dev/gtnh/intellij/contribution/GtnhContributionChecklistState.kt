package dev.gtnh.intellij.contribution

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

enum class GtnhHumanConfirmation { DEV_ENV_TESTED, FULLPACK_TESTED, AI_POLICY_ACKNOWLEDGED }

@Service(Service.Level.PROJECT)
@State(name = "GtnhContributionChecklist", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class GtnhContributionChecklistState : PersistentStateComponent<GtnhContributionChecklistState.Data> {
    data class Data(
        var devEnvTested: Boolean = false,
        var fullpackTested: Boolean = false,
        var aiPolicyAcknowledged: Boolean = false
    )

    private var data = Data()

    override fun getState(): Data = data
    override fun loadState(state: Data) { data = state }

    fun isConfirmed(item: GtnhHumanConfirmation): Boolean = when (item) {
        GtnhHumanConfirmation.DEV_ENV_TESTED -> data.devEnvTested
        GtnhHumanConfirmation.FULLPACK_TESTED -> data.fullpackTested
        GtnhHumanConfirmation.AI_POLICY_ACKNOWLEDGED -> data.aiPolicyAcknowledged
    }

    fun setConfirmed(item: GtnhHumanConfirmation, confirmed: Boolean) {
        when (item) {
            GtnhHumanConfirmation.DEV_ENV_TESTED -> data.devEnvTested = confirmed
            GtnhHumanConfirmation.FULLPACK_TESTED -> data.fullpackTested = confirmed
            GtnhHumanConfirmation.AI_POLICY_ACKNOWLEDGED -> data.aiPolicyAcknowledged = confirmed
        }
    }
}
