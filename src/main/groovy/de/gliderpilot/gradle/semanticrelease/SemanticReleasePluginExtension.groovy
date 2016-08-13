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
import org.ajoberstar.gradle.git.release.semver.StrategyUtil
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 * Created by tobias on 7/21/15.
 */
class SemanticReleasePluginExtension {

    final Project project
    final GitRepo repo
    final SemanticReleaseChangeLogService changeLog
    final SemanticReleaseCheckBranch releaseBranches
    final SemanticReleaseAppendBranchNameStrategy branchNames
    final SemanticReleaseNormalStrategy semanticStrategy
    final SemanticReleaseStrategy releaseStrategy
    final SemanticReleaseStrategy snapshotStrategy

    @Inject
    SemanticReleasePluginExtension(Project project) {
        this.project = project
        this.repo = new GithubRepo(project.grgit)
        changeLog = new SemanticReleaseChangeLogService(project.grgit, repo, project.release.tagStrategy)
        releaseBranches = new SemanticReleaseCheckBranch()
        branchNames = new SemanticReleaseAppendBranchNameStrategy(releaseBranches)
        semanticStrategy = new SemanticReleaseNormalStrategy(project.grgit, changeLog)
        releaseStrategy = new SemanticReleaseStrategy(
                initialStateService: new SemanticReleaseInitialStateService(project.grgit, project.release.tagStrategy),
                normalStrategy: semanticStrategy,
                createTag: true,
                selector: this.&isRelease
        )
        snapshotStrategy = releaseStrategy.copyWith(
                type: "SNAPSHOT",
                preReleaseStrategy: StrategyUtil.all(
                        branchNames,
                        appendSnapshot()
                ),
                createTag: false,
                selector: { true }
        )

    }

    def changeLog(Closure closure) {
        ConfigureUtil.configure(closure, changeLog)
    }

    def repo(Closure closure) {
        ConfigureUtil.configure(closure, repo)
    }

    def releaseBranches(Closure closure) {
        ConfigureUtil.configure(closure, releaseBranches)
    }

    def branchNames(Closure closure) {
        ConfigureUtil.configure(closure, branchNames)
    }

    boolean isRelease(SemVerStrategyState state) {
        !state.repoDirty && releaseBranches.isReleaseBranch(state.currentBranch.name) &&
                semanticStrategy.canRelease(state) && project.gradle.startParameter.taskNames.find { it == 'release' }
    }

    @Deprecated
    SemanticReleaseChangeLogService getChangeLogService() {
        project.logger.warn("semanticRelease.changeLogService is deprecated and will be removed in v2.0.0")
        changeLog
    }

    @Deprecated
    SemanticReleaseCheckBranch getOnReleaseBranch() {
        project.logger.warn("semanticRelease.onReleaseBranch is deprecated and will be removed in v2.0.0")
        releaseBranches
    }

    @Deprecated
    SemanticReleaseAppendBranchNameStrategy getAppendBranchName() {
        project.logger.warn("semanticRelease.appendBranchName is deprecated and will be removed in v2.0.0")
        branchNames
    }

    private PartialSemVerStrategy appendSnapshot() {
        return {
            it.inferredPreRelease ?
                    it.copyWith(inferredPreRelease: "${it.inferredPreRelease}-SNAPSHOT") :
                    it.copyWith(inferredPreRelease: "SNAPSHOT")
        } as PartialSemVerStrategy
    }

}
