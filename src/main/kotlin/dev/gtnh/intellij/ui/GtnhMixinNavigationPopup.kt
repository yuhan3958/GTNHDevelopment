package dev.gtnh.intellij.ui

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiClass
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import dev.gtnh.intellij.mixin.GtnhMixinLink
import javax.swing.JList

object GtnhMixinNavigationPopup {
    data class Target(val mixinClass: PsiClass, val links: List<GtnhMixinLink>)

    fun show(links: List<GtnhMixinLink>) {
        val targets = links.groupBy { it.mixinClass.qualifiedName ?: it.mixinClass }
            .values
            .map { Target(it.first().mixinClass, it) }

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(targets)
            .setTitle("GTNH Mixins Using This Config")
            .setRenderer(object : ColoredListCellRenderer<Target>() {
                override fun customizeCellRenderer(
                    list: JList<out Target>,
                    value: Target,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    icon = value.mixinClass.getIcon(0)
                    val kinds = value.links.map { it.kind.displayName }.distinct().joinToString("/")
                    append("[${kinds}] ${value.mixinClass.name ?: "<anonymous>"}")
                    value.mixinClass.qualifiedName
                        ?.substringBeforeLast('.', "")
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
                }
            })
            .setItemChosenCallback { it.mixinClass.navigate(true) }
            .createPopup()
            .showInFocusCenter()
    }
}
