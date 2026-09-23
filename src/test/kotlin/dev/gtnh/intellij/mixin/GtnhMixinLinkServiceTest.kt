package dev.gtnh.intellij.mixin

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase

class GtnhMixinLinkServiceTest : LightJavaCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.addClass(
            """
            package org.spongepowered.asm.mixin;
            public @interface Mixin { Class<?>[] value() default {}; }
            """.trimIndent()
        )
    }

    fun testBuilderRegistrationLinksBothDirections() {
        configureSources(
            source("example.Config", "public static boolean option = true;"),
            mixin("example.FooMixin"),
            source(
                "example.Registry",
                """
                static class Builder {
                    Builder addMixin(String name) { return this; }
                    Builder setApplyIf(java.util.function.BooleanSupplier condition) { return this; }
                }
                static final Builder REGISTRY = new Builder()
                    .addMixin("example.FooMixin")
                    .setApplyIf(() -> Config.option);
                """.trimIndent()
            )
        )

        assertBidirectional("example.Config", "option", "example.FooMixin")
    }

    fun testIfRegistrationWithClassLiteralLinksBothDirections() {
        configureSources(
            source("example.Config", "public static boolean option = true;"),
            mixin("example.FooMixin"),
            registry("if (Config.option) { register(FooMixin.class); }")
        )

        assertBidirectional("example.Config", "option", "example.FooMixin")
    }

    fun testIfStringRegistrationLinksBothDirections() {
        configureSources(
            source("example.Config", "public static boolean option = true;"),
            mixin("example.FooMixin"),
            registry("if (Config.option) { register(\"example.FooMixin\"); }")
        )

        assertBidirectional("example.Config", "option", "example.FooMixin")
    }

    fun testReverseLinkReturnsEveryControllingField() {
        configureSources(
            source("example.Config", "public static boolean foo = true; public static boolean bar = true;"),
            mixin("example.FooMixin"),
            registry("if (Config.foo && !Config.bar) { register(FooMixin.class); }")
        )

        val fields = service().findConfigsControllingMixin(psiClass("example.FooMixin"))
            .map { it.configField.name }
            .toSet()
        assertEquals(setOf("foo", "bar"), fields)
    }

    fun testUnconditionalRegistrationHasNoConfigLink() {
        configureSources(mixin("example.FooMixin"), registry("register(FooMixin.class);"))
        assertEmpty(service().findConfigsControllingMixin(psiClass("example.FooMixin")))
    }

    fun testAmbiguousSimpleStringDoesNotGuess() {
        configureSources(
            source("example.Config", "public static boolean option = true;"),
            mixin("one.FooMixin"),
            mixin("two.FooMixin"),
            registry("if (Config.option) { register(\"FooMixin\"); }")
        )

        val linked = service().findMixinsControlledBy(field("example.Config", "option"))
            .mapNotNull { it.mixinClass.qualifiedName }
            .toSet()
        assertEquals(setOf("one.FooMixin", "two.FooMixin"), linked)
    }

    fun testOrdinaryReferenceAndNonMixinClassProduceNoLinks() {
        configureSources(
            source("example.Config", "public static boolean option = true;"),
            source("example.NotMixin", ""),
            source("example.Gameplay", "boolean enabled() { return Config.option; }")
        )

        assertEmpty(service().findMixinsControlledBy(field("example.Config", "option")))
        assertEmpty(service().findConfigsControllingMixin(psiClass("example.NotMixin")))
    }

    fun testBrokenPsiDoesNotCrash() {
        myFixture.configureByText(
            "Broken.java",
            """
            package example;
            class Broken { void load() { if (Missing.config) register("FooMixin") }
            """.trimIndent()
        )
        assertEmpty(service().findMixinsControlledBy(myFixture.addClass(
            "package example; class Config { public static boolean unused; }"
        ).findFieldByName("unused", false)!!))
    }

    private fun assertBidirectional(configFqn: String, fieldName: String, mixinFqn: String) {
        val configField = field(configFqn, fieldName)
        val mixinClass = psiClass(mixinFqn)
        assertTrue(service().findMixinsControlledBy(configField).any { it.mixinClass.isEquivalentTo(mixinClass) })
        assertTrue(service().findConfigsControllingMixin(mixinClass).any { it.configField.isEquivalentTo(configField) })
    }

    private fun configureSources(vararg sources: Source) {
        sources.forEach { source -> myFixture.addClass(source.text) }
    }

    private fun source(fqn: String, body: String): Source {
        val packageName = fqn.substringBeforeLast('.')
        val className = fqn.substringAfterLast('.')
        return Source("package $packageName; public class $className { $body }")
    }

    private fun mixin(fqn: String): Source {
        val packageName = fqn.substringBeforeLast('.')
        val className = fqn.substringAfterLast('.')
        return Source(
            "package $packageName; @org.spongepowered.asm.mixin.Mixin(Object.class) public class $className {}"
        )
    }

    private fun registry(body: String): Source = Source(
        """
        package example;
        public class Registry {
            static void register(Class<?> type) {}
            static void register(String type) {}
            static void load() { $body }
        }
        """.trimIndent()
    )

    private fun field(classFqn: String, name: String): PsiField =
        psiClass(classFqn).findFieldByName(name, false)!!

    private fun psiClass(fqn: String): PsiClass = JavaPsiFacade.getInstance(project)
        .findClass(fqn, GlobalSearchScope.projectScope(project))!!

    private fun service(): GtnhMixinLinkService = GtnhMixinLinkService.getInstance(project)

    private data class Source(val text: String)
}
