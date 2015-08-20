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

import org.ajoberstar.gradle.git.release.semver.PartialSemVerStrategy
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 * Created by tobias on 7/21/15.
 */
class SemanticReleasePluginExtension {

    final Project project
    final Grgit grgit
    final SemanticReleaseChangeLogService changeLogService
    final SemanticReleaseCheckBranch onReleaseBranch
    final PartialSemVerStrategy appendBranchName
    final SemanticReleaseNormalStrategy semanticStrategy
    final SemanticReleaseStrategy releaseStrategy

    @Inject
    SemanticReleasePluginExtension(Project project) {
        this.project = project
        grgit = Grgit.open()
        changeLogService = new SemanticReleaseChangeLogService()
        onReleaseBranch = new SemanticReleaseCheckBranch()
        appendBranchName = new SemanticReleaseAppendBranchNameStrategy()
        semanticStrategy = new SemanticReleaseNormalStrategy(grgit, changeLogService, project.release.tagStrategy)
        releaseStrategy = new SemanticReleaseStrategy(
                normalStrategy: semanticStrategy,
                createTag: true,
                selector: {
                    !it.repoDirty &&
                            onReleaseBranch.isReleaseBranch(it.currentBranch.name) &&
                            semanticStrategy.doRelease(it) &&
                            isRelease()
                }
        )
    }

    def changeLog(Closure closure) {
        ConfigureUtil.configure(closure, changeLogService)
    }

    def releaseBranches(Closure closure) {
        ConfigureUtil.configure(closure, onReleaseBranch)
    }

    def branchNames(Closure closure) {
        ConfigureUtil.configure(closure, appendBranchName)
    }

    boolean isRelease() {
        project.gradle.startParameter.taskNames.find { it == 'release' }
    }

}
