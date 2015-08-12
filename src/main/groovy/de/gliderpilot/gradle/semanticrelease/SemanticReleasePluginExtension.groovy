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
import org.ajoberstar.gradle.git.release.semver.SemVerStrategy
import org.ajoberstar.gradle.git.release.semver.StrategyUtil
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 * Created by tobias on 7/21/15.
 */
class SemanticReleasePluginExtension {

    final Grgit grgit
    final SemanticReleaseCommitMessageConventions commitMessageConventions
    final PartialSemVerStrategy semanticStrategy
    final PartialSemVerStrategy onReleaseBranch
    final PartialSemVerStrategy appendBranchName

    @Inject
    SemanticReleasePluginExtension(Project project) {
        grgit = Grgit.open()
        commitMessageConventions = new SemanticReleaseCommitMessageConventions()
        semanticStrategy = new SemanticReleaseNormalStrategy(grgit, commitMessageConventions, project.release.tagStrategy)
        onReleaseBranch = new SemanticReleaseCheckBranch()
        appendBranchName = new SemanticReleaseAppendBranchNameStrategy()
    }

    def commitMessages(Closure closure) {
        ConfigureUtil.configure(closure, commitMessageConventions)
    }

    def releaseBranches(Closure closure) {
        ConfigureUtil.configure(closure, onReleaseBranch)
    }

    def branchNames(Closure closure) {
        ConfigureUtil.configure(closure, appendBranchName)
    }

    SemVerStrategy toSemanticReleaseStrategy(SemVerStrategy strategy) {
        strategy.copyWith(
                normalStrategy: semanticStrategy,
                preReleaseStrategy: StrategyUtil.all(appendBranchName, strategy.preReleaseStrategy),
                buildMetadataStrategy: StrategyUtil.all(strategy.buildMetadataStrategy, onReleaseBranch)
        )
    }

}
