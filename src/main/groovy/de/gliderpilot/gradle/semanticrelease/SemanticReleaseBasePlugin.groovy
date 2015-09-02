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

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.gradle.git.release.base.BaseReleasePlugin
import org.ajoberstar.gradle.git.release.base.ReleasePluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class SemanticReleaseBasePlugin implements Plugin<Project> {

    void apply(Project project) {
        project.with {
            plugins.apply('org.ajoberstar.grgit')
            plugins.apply('org.ajoberstar.release-base')
            SemanticReleasePluginExtension semanticRelease = extensions.create("semanticRelease", SemanticReleasePluginExtension, project)
            ReleasePluginExtension releaseExtension = project.extensions.findByType(ReleasePluginExtension)
            releaseExtension.with {
                tagStrategy {
                    generateMessage = { version ->
                        String previousVersion = Version.valueOf(version.previousVersion).majorVersion ? version.previousVersion : null
                        String previousTag = (previousVersion && releaseExtension.tagStrategy.prefixNameWithV) ? "v$previousVersion" : previousVersion
                        String currentTag = releaseExtension.tagStrategy.prefixNameWithV ? "v$version.version" : version.version
                        semanticRelease.changeLogService.changeLog(
                                grgit,
                                previousTag,
                                currentTag,
                                version.version,
                                semanticRelease.changeLogService.commits(grgit, Version.valueOf(version.previousVersion))).toString()
                    }
                }
            }
            tasks.release.doLast {
                if (project.version.inferredVersion.createTag) {
                    String tag = releaseExtension.tagStrategy.prefixNameWithV ? "v$project.version" : "$project.version"
                    semanticRelease.changeLogService.createGitHubVersion(grgit, tag, project.release.tagStrategy.generateMessage(project.version.inferredVersion))
                }
            }
        }
    }
}
