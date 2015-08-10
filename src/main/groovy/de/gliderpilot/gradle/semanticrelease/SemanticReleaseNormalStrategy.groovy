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
import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.TagStrategy
import org.ajoberstar.gradle.git.release.base.VersionStrategy
import org.ajoberstar.gradle.git.release.semver.*
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project

class SemanticReleaseNormalStrategy implements VersionStrategy {

    final Grgit grgit
    final SemanticReleaseCommitMessageConventions commitMessageConventions
    final TagStrategy tagStrategy

    SemanticReleaseNormalStrategy(Grgit grgit,
                                  SemanticReleaseCommitMessageConventions commitMessageConventions,
                                  TagStrategy tagStrategy) {
        this.grgit = grgit
        this.commitMessageConventions = commitMessageConventions
        this.tagStrategy = tagStrategy
    }
    
    @Override
    SemVerStrategyState infer(SemVerStrategyState initialState) {
        NearestVersion nearestVersion = initialState.nearestVersion
        Version previousVersion = nearestVersion.normal

        if (previousVersion.majorVersion && !nearestVersion.distanceFromNormal) {
            // nothing has changed since last version
            return initialState
        }

        List<Commit> log = grgit.log() {
            if (previousVersion.majorVersion) {
                String previousVersionString = (tagStrategy.prefixNameWithV ? 'v' : '') + previousVersion.toString()
                // range previousVersionString, 'HEAD' does not work: https://github.com/ajoberstar/grgit/issues/71
                range "${previousVersionString}^{commit}".toString(), 'HEAD'
            }
        }

        SemVerStrategyState state = initialState

        if (log.any { commitMessageConventions.type(it) == 'fix' })
            state = StrategyUtil.incrementNormalFromScope(initialState, ChangeScope.PATCH)
        if (log.any { commitMessageConventions.type(it) == 'feat' })
            state = StrategyUtil.incrementNormalFromScope(initialState, ChangeScope.MINOR)
        if (log.any(commitMessageConventions.&breaks))
            state = StrategyUtil.incrementNormalFromScope(initialState, ChangeScope.MAJOR)
        if (state != initialState && !previousVersion.majorVersion)
            state = initialState.copyWith(inferredNormal: '1.0.0')

        return state
    }
}
