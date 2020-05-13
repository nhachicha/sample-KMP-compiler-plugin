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
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class AccessorModifierGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(AccessorModifierGradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {}
}

@AutoService(KotlinGradleSubplugin::class)
class AccessorModifierKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val ARTIFACT_GROUP_NAME = "dev.nhachicha"
        const val ARTIFACT_SHADED_NAME = "accessor-modifier-compiler-plugin"
        const val ARTIFACT_UNSHADED_NAME = "accessor-modifier-compiler-plugin-unshaded"
        const val ARTIFACT_VERSION = "0.0.1-SNAPSHOT"
        const val PLUGIN_ID = "dev.nhachicha.accessor-modifier-compiler-plugin"
    }

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
            AccessorModifierGradleSubplugin.isEnabled(project)

    override fun apply(
            project: Project,
            kotlinCompile: AbstractCompile,
            javaCompile: AbstractCompile?,
            variantData: Any?,
            androidProjectHandler: Any?,
            kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {
        return emptyList()
    }

    override fun getPluginArtifact(): SubpluginArtifact =
            SubpluginArtifact(ARTIFACT_GROUP_NAME, ARTIFACT_SHADED_NAME, ARTIFACT_VERSION)

    override fun getNativeCompilerPluginArtifact(): SubpluginArtifact? =
            SubpluginArtifact(ARTIFACT_GROUP_NAME, ARTIFACT_UNSHADED_NAME, ARTIFACT_VERSION)

    override fun getCompilerPluginId() = PLUGIN_ID
}
