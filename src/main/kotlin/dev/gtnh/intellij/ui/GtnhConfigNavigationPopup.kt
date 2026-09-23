package dev.gtnh.intellij.ui

import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiField
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import dev.gtnh.intellij.mixin.GtnhMixinLink
import javax.swing.JList

object GtnhConfigNavigationPopup {
    data class Target(val field: PsiField, val links: List<GtnhMixinLink>)

    fun show(links: List<GtnhMixinLink>) {
        val targets = links.flatMap { link -> link.configFields.map { field -> field to link } }
            .groupBy({ (field, _) -> field.containingClass?.qualifiedName to field.name }, { it })
            .values.map { entries -> Target(entries.first().first, entries.map { it.second }) }

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(targets)
            .setTitle("GTNH Configs Controlling This Mixin")
            .setRenderer(object : ColoredListCellRenderer<Target>() {
                override fun customizeCellRenderer(
                    list: JList<out Target>, value: Target, index: Int, selected: Boolean, hasFocus: Boolean
                ) {
                    icon = value.field.getIcon(0)
                    val owner = value.field.containingClass
                    val kinds = value.links.map { it.kind.displayName }.distinct().joinToString("/")
                    append("[$kinds] ${owner?.name ?: "<anonymous>"}.${value.field.name}")
                    owner?.qualifiedName?.substringBeforeLast('.', "")?.takeIf(String::isNotEmpty)
                        ?.let { append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
                }
            })
            .setItemChosenCallback { it.field.navigate(true) }
            .createPopup()
            .showInFocusCenter()
    }
}
