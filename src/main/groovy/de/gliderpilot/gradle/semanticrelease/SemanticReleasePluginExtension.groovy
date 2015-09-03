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
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 * Created by tobias on 7/21/15.
 */
class SemanticReleasePluginExtension {

    final Project project
    final SemanticReleaseChangeLogService changeLogService
    final SemanticReleaseCheckBranch onReleaseBranch
    final SemanticReleaseAppendBranchNameStrategy appendBranchName
    final SemanticReleaseNormalStrategy semanticStrategy
    final SemanticReleaseStrategy releaseStrategy

    @Inject
    SemanticReleasePluginExtension(Project project) {
        this.project = project
        changeLogService = new SemanticReleaseChangeLogService(project.release.tagStrategy)
        onReleaseBranch = new SemanticReleaseCheckBranch()
        appendBranchName = new SemanticReleaseAppendBranchNameStrategy(onReleaseBranch)
        semanticStrategy = new SemanticReleaseNormalStrategy(project.grgit, changeLogService)
        releaseStrategy = new SemanticReleaseStrategy(
                normalStrategy: semanticStrategy,
                createTag: true,
                selector: this.&isRelease
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

    boolean isRelease(SemVerStrategyState state) {
        !state.repoDirty && onReleaseBranch.isReleaseBranch(state.currentBranch.name) &&
                semanticStrategy.doRelease(state) && project.gradle.startParameter.taskNames.find { it == 'release' }
    }

}
