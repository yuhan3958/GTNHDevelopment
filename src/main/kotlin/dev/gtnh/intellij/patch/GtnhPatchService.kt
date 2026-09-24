package dev.gtnh.intellij.patch

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import dev.gtnh.intellij.patch.provider.AccessTransformerPatchProvider
import dev.gtnh.intellij.patch.provider.AsmTransformerPatchProvider
import dev.gtnh.intellij.patch.provider.MixinPatchProvider
import dev.gtnh.intellij.settings.GtnhSettingsState

@Service(Service.Level.PROJECT)
class GtnhPatchService @JvmOverloads constructor(
    private val project: Project,
    private val providers: List<GtnhPatchProvider> = defaultProviders(project)
) {
    fun findPatches(target: GtnhPatchTarget): List<GtnhPatch> {
        val patches = ArrayList<GtnhPatch>()
        providers.forEach { provider ->
            ProgressManager.checkCanceled()
            try {
                patches += provider.findPatches(target)
            } catch (canceled: ProcessCanceledException) {
                throw canceled
            } catch (failure: RuntimeException) {
                LOG.debug("GTNH patch provider failed: ${provider.javaClass.name}", failure)
            }
        }
        return patches.distinctBy {
            listOf(it.kind, it.confidence, it.detail, it.source.toString(), it.target.targetMethod?.name)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(GtnhPatchService::class.java)

        fun getInstance(project: Project): GtnhPatchService = project.getService(GtnhPatchService::class.java)

        private fun defaultProviders(project: Project): List<GtnhPatchProvider> = buildList {
            add(MixinPatchProvider(project))
            if (GtnhSettingsState.getInstance().state.patchHeuristics) {
                add(AsmTransformerPatchProvider(project))
                add(AccessTransformerPatchProvider(project))
            }
        }
    }
}
