package dev.gtnh.intellij.mixin

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement

class GtnhMixinConfigLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val mixinClass = element.parent as? PsiClass ?: return null
        if (mixinClass.nameIdentifier !== element || !GtnhMixinUtil.isMixinClass(mixinClass)) return null
        val links = GtnhMixinLinkService.getInstance(element.project)
            .cachedConfigsControllingMixin(mixinClass)
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
            .setTargets(links.flatMap { link -> link.configFields.mapNotNull { it.element } })
            .setTooltipText("Navigate to controlling GTNH config")
            .createLineMarkerInfo(element)
    }
}
