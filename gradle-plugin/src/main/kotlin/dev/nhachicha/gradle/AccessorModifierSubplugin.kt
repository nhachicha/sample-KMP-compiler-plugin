/*
 * Copyright 2020 Nabil Hachicha.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.nhachicha.gradle

import com.google.auto.service.AutoService
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class AccessorModifierGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(AccessorModifierKotlinGradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {}
}

@AutoService(KotlinCompilerPluginSupportPlugin::class)
class AccessorModifierKotlinGradleSubplugin : KotlinCompilerPluginSupportPlugin {
    companion object {
        const val ARTIFACT_GROUP_NAME = "dev.nhachicha"
        const val ARTIFACT_NAME = "accessor-modifier-compiler-plugin"
        const val ARTIFACT_SHADED_NAME = "accessor-modifier-compiler-plugin-shaded"
        const val ARTIFACT_VERSION = "0.0.1-SNAPSHOT"
        const val PLUGIN_ID = "dev.nhachicha.accessor-modifier-compiler-plugin"
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean =
            AccessorModifierGradleSubplugin.isEnabled(kotlinCompilation.target.project)

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        return project.provider {
            listOf(SubpluginOption(key = "optio-key", value = "option-value"))
        }
    }

    override fun getPluginArtifact(): SubpluginArtifact =
            SubpluginArtifact(ARTIFACT_GROUP_NAME, ARTIFACT_NAME, ARTIFACT_VERSION)

    override fun getPluginArtifactForNative(): SubpluginArtifact? =
            SubpluginArtifact(ARTIFACT_GROUP_NAME, ARTIFACT_SHADED_NAME, ARTIFACT_VERSION)

    override fun getCompilerPluginId() = PLUGIN_ID
}
