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

import groovy.transform.PackageScope
import org.ajoberstar.gradle.git.release.semver.PartialSemVerStrategy
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import org.gradle.api.GradleException

/**
 * Created by tobias on 7/21/15.
 */
@PackageScope
class GradleSemanticReleaseCheckReleaseBranchStrategy implements PartialSemVerStrategy {

    Set<String> includes = [/^master$/, /^(?:release[-\/])?\d+(?:\.\d+)?\.x$/] as Set
    Set<String> excludes = [] as Set

    void include(String... patterns) {
        includes.addAll(patterns)
    }

    void exclude(String... patterns) {
        excludes.addAll(patterns)
    }

    @Override
    SemVerStrategyState infer(SemVerStrategyState state) {
        if (state.inferredPreRelease || state.inferredBuildMetadata)
            return state

        String branchName = state.currentBranch.name

        if (!(includes.isEmpty() || includes.any { branchName ==~ it })) {
            throw new GradleException("${branchName} does not match one of the included patterns: ${includes}")
        }

        if (excludes.any { branchName ==~ it }) {
            throw new GradleException("${branchName} matched an excluded pattern: ${excludes}")
        }
        return state
    }

}
