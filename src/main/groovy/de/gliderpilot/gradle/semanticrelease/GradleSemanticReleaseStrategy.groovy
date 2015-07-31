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
import org.ajoberstar.gradle.git.release.base.TagStrategy
import org.ajoberstar.gradle.git.release.semver.*
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.util.JGitUtil

class GradleSemanticReleaseStrategy implements PartialSemVerStrategy {

    final Grgit grgit
    final GradleSemanticReleaseCommitMessageConventions commitMessageConventions
    final TagStrategy tagStrategy

    GradleSemanticReleaseStrategy(Grgit grgit,
                                  GradleSemanticReleaseCommitMessageConventions commitMessageConventions,
                                  TagStrategy tagStrategy) {
        this.grgit = grgit
        this.commitMessageConventions = commitMessageConventions
        this.tagStrategy = tagStrategy
    }

    @Override
    SemVerStrategyState infer(SemVerStrategyState state) {
        NearestVersion nearestVersion = state.nearestVersion
        Version previousVersion = nearestVersion?.normal
        if (!previousVersion || !previousVersion.majorVersion)
            return state.copyWith(inferredNormal: '1.0.0')

        if (!nearestVersion.distanceFromNormal) {
            // nothing has changed
            return state
        }

        List<Commit> log = grgit.log() {
            String previousVersionString = (tagStrategy.prefixNameWithV ? 'v' : '') + previousVersion.toString()
            // range previousVersionString, 'HEAD' does not work: https://github.com/ajoberstar/grgit/issues/71
            Tag previousVersionTag = JGitUtil.resolveTag(grgit.repository, previousVersionString)
            range previousVersionTag.commit, 'HEAD'
        }

        if (log.any(commitMessageConventions.&breaks))
            return StrategyUtil.incrementNormalFromScope(state, ChangeScope.MAJOR)
        if (log.any { commitMessageConventions.type(it) == 'feat' })
            return StrategyUtil.incrementNormalFromScope(state, ChangeScope.MINOR)
        return StrategyUtil.incrementNormalFromScope(state, ChangeScope.PATCH)
    }
}
