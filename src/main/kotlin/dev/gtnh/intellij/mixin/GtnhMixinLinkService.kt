package dev.gtnh.intellij.mixin

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiField
import com.intellij.psi.PsiClass
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiModificationTracker
import dev.gtnh.intellij.mixin.provider.BuilderMixinLinkProvider
import dev.gtnh.intellij.mixin.provider.ConditionalMixinLinkProvider
import dev.gtnh.intellij.mixin.provider.DirectMixinLinkProvider

@Service(Service.Level.PROJECT)
class GtnhMixinLinkService(private val project: Project) {
    private val resolver = GtnhMixinClassResolver(project)
    private val registrationFinder = GtnhMixinRegistrationFinder(project)
    private data class CacheEntry(val modificationCount: Long, val links: List<GtnhMixinLinkSnapshot>)

    private val reverseCache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()
    private val providers: List<GtnhMixinLinkProvider> = listOf(
        ConditionalMixinLinkProvider(resolver),
        BuilderMixinLinkProvider(resolver),
        DirectMixinLinkProvider(resolver)
    )

    fun findMixinsControlledBy(field: PsiField): List<GtnhMixinLink> {
        val references = GtnhConfigUsageFinder.findUsages(project, field)
        return providers.flatMap { it.findByConfig(field, references) }
            .distinctBy { link ->
                listOf(
                    link.mixinClass.qualifiedName ?: link.mixinClass.name,
                    link.kind,
                    link.registrationElement?.containingFile?.virtualFile?.path,
                    link.registrationElement?.textOffset
                )
            }
    }

    fun findConfigsControllingMixin(mixinClass: PsiClass): List<GtnhMixinLink> {
        if (!GtnhMixinUtil.isMixinClass(mixinClass)) return emptyList()
        val registrations = registrationFinder.findRegistrations(mixinClass)
        val links = providers.flatMap { it.findByMixin(mixinClass, registrations) }
            .distinctBy { link ->
                listOf(
                    link.configFields.map { it.containingClass?.qualifiedName to it.name },
                    link.mixinClass.qualifiedName,
                    link.registrationElement?.containingFile?.virtualFile?.path,
                    link.registrationElement?.textOffset,
                    link.kind
                )
            }
        mixinClass.qualifiedName?.let { key ->
            val pointerManager = SmartPointerManager.getInstance(project)
            reverseCache[key] = CacheEntry(
                PsiModificationTracker.getInstance(project).modificationCount,
                links.map { link ->
                    GtnhMixinLinkSnapshot(
                        link.configFields.map(pointerManager::createSmartPsiElementPointer),
                        pointerManager.createSmartPsiElementPointer(link.mixinClass),
                        link.kind
                    )
                }
            )
        }
        return links
    }

    fun cachedConfigsControllingMixin(mixinClass: PsiClass): List<GtnhMixinLinkSnapshot>? {
        val key = mixinClass.qualifiedName ?: return null
        val entry = reverseCache[key] ?: return null
        if (entry.modificationCount != PsiModificationTracker.getInstance(project).modificationCount) {
            reverseCache.remove(key, entry)
            return null
        }
        return entry.links.takeIf { links ->
            links.all { link ->
                link.configFields.all { it.element?.isValid == true } && link.mixinClass.element?.isValid == true
            }
        }
    }

    companion object {
        fun getInstance(project: Project): GtnhMixinLinkService = project.getService(GtnhMixinLinkService::class.java)
    }
}
