package dev.gtnh.intellij.patch.provider

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import dev.gtnh.intellij.patch.GtnhPatchConfidence
import dev.gtnh.intellij.patch.GtnhPatchKind
import dev.gtnh.intellij.patch.GtnhPatchTarget

class PatchProvidersTest : LightJavaCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.addClass("package org.spongepowered.asm.mixin; public @interface Mixin { Class<?>[] value(); }")
        myFixture.addClass("package org.spongepowered.asm.mixin.injection; public @interface Inject { String method(); }")
    }

    fun testMixinInjectIsExactAndMethodAware() {
        val target = myFixture.addClass("package game; public class Target { void tick() {} void other() {} }")
        myFixture.addClass(
            """
            package mod;
            @org.spongepowered.asm.mixin.Mixin(game.Target.class)
            public class TargetMixin {
                @org.spongepowered.asm.mixin.injection.Inject(method="tick()V") void patch() {}
            }
            """.trimIndent()
        )
        val method = target.findMethodsByName("tick", false).single()
        val patches = MixinPatchProvider(project).findPatches(GtnhPatchTarget(target, method))
        assertEquals(1, patches.size)
        assertEquals(GtnhPatchKind.MIXIN_INJECT, patches.single().kind)
        assertEquals(GtnhPatchConfidence.EXACT, patches.single().confidence)
        assertEquals("tick()V", patches.single().detail)
    }

    fun testAsmStringRequiresTransformerContext() {
        val target = myFixture.addClass("package game; public class Target {}")
        myFixture.addClass("package mod; public class Ordinary { String name = \"game.Target\"; }")
        myFixture.addClass(
            """
            package mod;
            import org.objectweb.asm.ClassReader;
            public class Transformer { Object transform() { return "game.Target"; } }
            """.trimIndent()
        )
        val patches = AsmTransformerPatchProvider(project).findPatches(GtnhPatchTarget(target))
        assertEquals(1, patches.size)
        assertEquals(GtnhPatchConfidence.HEURISTIC, patches.single().confidence)
    }

    fun testAccessTransformerRowsAreHeuristicAndMalformedRowsIgnored() {
        val target = myFixture.addClass("package game; public class Target { void tick() {} }")
        myFixture.addFileToProject(
            "META-INF/example_at.cfg",
            "# comment\nmalformed\npublic game.Target tick()V\n"
        )
        val patches = AccessTransformerPatchProvider(project).findPatches(
            GtnhPatchTarget(target, target.findMethodsByName("tick", false).single())
        )
        assertEquals(1, patches.size)
        assertEquals(GtnhPatchKind.ACCESS_TRANSFORMER, patches.single().kind)
        assertEquals(GtnhPatchConfidence.HEURISTIC, patches.single().confidence)
    }
}
