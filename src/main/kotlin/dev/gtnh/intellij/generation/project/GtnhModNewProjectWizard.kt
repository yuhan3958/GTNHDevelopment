package dev.gtnh.intellij.generation.project

import com.intellij.DynamicBundle
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.ide.wizard.newProjectWizardBaseStepWithoutGap
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.layout.ValidationInfoBuilder
import org.jetbrains.plugins.gradle.service.project.open.GradleOpenProjectProvider
import java.nio.file.Path
import javax.swing.Icon

class GtnhModNewProjectWizard : GeneratorNewProjectWizard {
    override val id: String = "GTNHMod"
    override val name: String = GtnhWizardBundle.message("gtnh.wizard.name")
    override val icon: Icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", GtnhModNewProjectWizard::class.java)

    override fun createStep(context: WizardContext): NewProjectWizardStep =
        RootNewProjectWizardStep(context)
            .nextStep(::newProjectWizardBaseStepWithoutGap)
            .nextStep(::MetadataStep)

    private class MetadataStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
        private val baseData = NewProjectWizardBaseData.getBaseData(this)
        private val modNameProperty = propertyGraph.property(baseData?.name.orEmpty())
        private val modIdProperty = propertyGraph.property("")
        private val modGroupProperty = propertyGraph.property("")

        private var modName by modNameProperty
        private var modId by modIdProperty
        private var modGroup by modGroupProperty

        override fun setupUI(builder: Panel) {
            with(builder) {
                row(GtnhWizardBundle.message("gtnh.wizard.modName")) {
                    textField()
                        .bindText(modNameProperty)
                        .columns(COLUMNS_MEDIUM)
                        .validationOnInput { validateModName() }
                        .validationOnApply { validateModName() }
                }
                row(GtnhWizardBundle.message("gtnh.wizard.modId")) {
                    textField()
                        .bindText(modIdProperty)
                        .columns(COLUMNS_MEDIUM)
                        .validationOnInput { validateModId() }
                        .validationOnApply { validateModId() }
                }
                row(GtnhWizardBundle.message("gtnh.wizard.modGroup")) {
                    textField()
                        .bindText(modGroupProperty)
                        .columns(COLUMNS_MEDIUM)
                        .validationOnInput { validateModGroup() }
                        .validationOnApply { validateModGroup() }
                }
            }
        }

        override fun setupProject(project: Project) {
            val model = GtnhModProjectModel(modName.trim(), modId.trim(), modGroup.trim())
            model.validationError()?.let { throw ConfigurationException(it) }

            try {
                val completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    Runnable {
                        GtnhStarterInstaller().install(Path.of(context.projectFileDirectory), model) {
                            ProgressManager.checkCanceled()
                        }
                    },
                    GtnhWizardBundle.message("gtnh.wizard.progress"),
                    true,
                    project
                )
                if (!completed) throw ProcessCanceledException()
                GradleOpenProjectProvider().linkToExistingProject(context.projectFileDirectory, project)
            } catch (error: ProcessCanceledException) {
                throw error
            } catch (error: GtnhStarterInstallationException) {
                val phaseKey = when (error.phase) {
                    GtnhStarterInstallationPhase.DOWNLOAD -> "gtnh.wizard.error.download"
                    GtnhStarterInstallationPhase.EXTRACTION -> "gtnh.wizard.error.extraction"
                    GtnhStarterInstallationPhase.CUSTOMIZATION -> "gtnh.wizard.error.customization"
                    GtnhStarterInstallationPhase.PUBLISH -> "gtnh.wizard.error.publish"
                }
                throw ConfigurationException(
                    "${GtnhWizardBundle.message(phaseKey)}: ${error.message}",
                    error,
                    GtnhWizardBundle.message("gtnh.wizard.errorTitle")
                )
            } catch (error: Exception) {
                throw ConfigurationException(
                    GtnhWizardBundle.message("gtnh.wizard.error", error.message ?: error.javaClass.simpleName),
                    error,
                    GtnhWizardBundle.message("gtnh.wizard.errorTitle")
                )
            }
        }

        private fun ValidationInfoBuilder.validateModName() =
            when {
                modName.isBlank() -> error(GtnhWizardBundle.message("gtnh.wizard.validation.modName.required"))
                GtnhModProjectModel(modName.trim(), "valid", "dev.example.valid").validationError() != null ->
                    error(GtnhWizardBundle.message("gtnh.wizard.validation.modName.invalid"))
                else -> null
            }

        private fun ValidationInfoBuilder.validateModId() =
            if (GtnhModProjectModel("Valid", modId.trim(), "dev.example.valid").validationError() == null) null
            else error(GtnhWizardBundle.message("gtnh.wizard.validation.modId"))

        private fun ValidationInfoBuilder.validateModGroup() =
            if (GtnhModProjectModel("Valid", "valid", modGroup.trim()).validationError() == null) null
            else error(GtnhWizardBundle.message("gtnh.wizard.validation.modGroup"))
    }
}

private object GtnhWizardBundle : DynamicBundle(GtnhWizardBundle::class.java, "messages.MyMessageBundle") {
    fun message(key: String, vararg params: Any): String = getMessage(key, *params)
}
