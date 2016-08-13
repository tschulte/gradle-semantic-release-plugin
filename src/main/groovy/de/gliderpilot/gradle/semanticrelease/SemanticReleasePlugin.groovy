/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gliderpilot.gradle.semanticrelease

import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.plugins.PublishingPlugin

class SemanticReleasePlugin implements Plugin<Project> {

    void apply(Project project) {
        project.with {
            plugins.apply(org.ajoberstar.gradle.git.base.GrgitPlugin)
            plugins.apply(org.ajoberstar.gradle.git.release.base.BaseReleasePlugin)
            SemanticReleasePluginExtension semanticReleaseExtension = extensions.create("semanticRelease", SemanticReleasePluginExtension, project)
            ReleasePluginExtension releaseExtension = extensions.findByType(ReleasePluginExtension)
            def releaseTask = tasks.release
            tasks.create("updateGithubRelease", UpdateGithubRelease)
            releaseTask.finalizedBy project.tasks.updateGithubRelease
            releaseExtension.with {
                versionStrategy semanticReleaseExtension.releaseStrategy
                defaultVersionStrategy = semanticReleaseExtension.snapshotStrategy
            }
            allprojects { prj ->
                prj.plugins.withType(JavaBasePlugin) {
                    releaseTask.dependsOn prj.tasks.build
                }
                prj.plugins.withType(BasePlugin) {
                    releaseTask.finalizedBy prj.tasks.uploadArchives
                }
                prj.plugins.withType(PublishingPlugin) {
                    releaseTask.finalizedBy prj.tasks.publish
                }
            }
        }
    }
}
