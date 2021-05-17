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

import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class UpdateGithubRelease extends DefaultTask {

    private final Logger logger = Logging.getLogger(getClass())

    UpdateGithubRelease() {
        onlyIf { repo.mnemo }
        onlyIf { repo.github }
        onlyIf { version?.createTag }
        dependsOn { repo.releaseAssets*.builtBy.findAll { it != null } }
    }

    @Input
    protected ReleaseVersion getVersion() {
        project.version.inferredVersion
    }

    @Internal
    protected String getTagName() {
        project.release.tagStrategy.toTagString(version.version)
    }

    @Input
    Collection<Asset> getReleaseAssets() {
        return repo.releaseAssets
    }

    @Internal
    GithubRepo getRepo() {
        project.semanticRelease.repo
    }

    @Internal
    SemanticReleaseChangeLogService getChangeLog() {
        project.semanticRelease.changeLog
    }

    @TaskAction
    void updateGithubRelease() {
        new UpdateGithubReleaseService().updateGithubRelease(changeLog, repo, version, tagName)
    }
}
