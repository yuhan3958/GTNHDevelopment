package dev.gtnh.intellij.patch

import com.intellij.pom.Navigatable

enum class GtnhPatchConfidence { EXACT, HEURISTIC }

enum class GtnhPatchKind(val displayName: String) {
    MIXIN_INJECT("Mixin @Inject"),
    MIXIN_REDIRECT("Mixin @Redirect"),
    MIXIN_MODIFY_CONSTANT("Mixin @ModifyConstant"),
    MIXIN_MODIFY_VARIABLE("Mixin @ModifyVariable"),
    MIXIN_OVERWRITE("Mixin @Overwrite"),
    MIXIN_SHADOW("Mixin @Shadow"),
    MIXIN_EXTRAS("MixinExtras"),
    ASM_TRANSFORMER("ASM transformer"),
    ACCESS_TRANSFORMER("Access Transformer")
}

data class GtnhPatch(
    val target: GtnhPatchTarget,
    val source: Navigatable,
    val kind: GtnhPatchKind,
    val confidence: GtnhPatchConfidence,
    val detail: String? = null,
    val controllingConfigs: List<String> = emptyList()
)
