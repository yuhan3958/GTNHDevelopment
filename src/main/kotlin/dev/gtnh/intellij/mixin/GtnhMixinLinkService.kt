package dev.gtnh.intellij.mixin

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiField
import dev.gtnh.intellij.mixin.provider.BuilderMixinLinkProvider
import dev.gtnh.intellij.mixin.provider.ConditionalMixinLinkProvider
import dev.gtnh.intellij.mixin.provider.DirectMixinLinkProvider

@Service(Service.Level.PROJECT)
class GtnhMixinLinkService(private val project: Project) {
    private val resolver = GtnhMixinClassResolver(project)
    private val providers: List<GtnhMixinLinkProvider> = listOf(
        ConditionalMixinLinkProvider(resolver),
        BuilderMixinLinkProvider(resolver),
        DirectMixinLinkProvider(resolver)
    )

    fun findMixinsControlledBy(field: PsiField): List<GtnhMixinLink> {
        val references = GtnhConfigUsageFinder.findUsages(project, field)
        return providers.flatMap { it.findMixinsControlledBy(field, references) }
            .distinctBy { link ->
                listOf(
                    link.mixinClass.qualifiedName ?: link.mixinClass.name,
                    link.kind,
                    link.registrationElement?.containingFile?.virtualFile?.path,
                    link.registrationElement?.textOffset
                )
            }
    }

    companion object {
        fun getInstance(project: Project): GtnhMixinLinkService = project.getService(GtnhMixinLinkService::class.java)
    }
}
