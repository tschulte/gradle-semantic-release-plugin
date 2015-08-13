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
import groovy.transform.Memoized
import org.ajoberstar.gradle.git.release.base.TagStrategy
import org.ajoberstar.gradle.git.release.semver.*
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit

class SemanticReleaseNormalStrategy implements PartialSemVerStrategy {

    private final Grgit grgit
    private final SemanticReleaseCommitMessageConventions commitMessageConventions
    private final TagStrategy tagStrategy

    SemanticReleaseNormalStrategy(Grgit grgit,
                                  SemanticReleaseCommitMessageConventions commitMessageConventions,
                                  TagStrategy tagStrategy) {
        this.grgit = grgit
        this.commitMessageConventions = commitMessageConventions
        this.tagStrategy = tagStrategy
    }

    @Override
    SemVerStrategyState infer(SemVerStrategyState initialState) {
        doInfer(initialState)
    }

    /**
     * @return true if there where fixes, features or breaking changes, false, if there where no commits or if these commits
     */
    boolean doRelease(SemVerStrategyState initialState) {
        doInfer(initialState) != initialState
    }

    /*
     * Memoized does not work together with @Override
     */

    @Memoized
    private SemVerStrategyState doInfer(SemVerStrategyState initialState) {
        NearestVersion nearestVersion = initialState.nearestVersion
        Version previousVersion = nearestVersion.normal

        if (previousVersion.majorVersion && !nearestVersion.distanceFromNormal) {
            // nothing has changed since last version
            return initialState
        }

        List<Commit> log = grgit.log {
            includes << 'HEAD'
            if (previousVersion.majorVersion) {
                String previousVersionString = (tagStrategy.prefixNameWithV ? 'v' : '') + previousVersion.toString()
                // range previousVersionString, 'HEAD' does not work: https://github.com/ajoberstar/grgit/issues/71
                excludes << "${previousVersionString}^{commit}".toString()
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
