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

/**
 * Created by tobias on 7/21/15.
 */
@PackageScope
class SemanticReleaseAppendBranchNameStrategy implements PartialSemVerStrategy {

    def replacePatterns = [:]

    SemanticReleaseAppendBranchNameStrategy() {
        replace(~/^feature[-\/](.*)$/, '$1')
    }

    void replace(pattern, replacement) {
        replacePatterns[pattern] = replacement
    }

    @Override
    SemVerStrategyState infer(SemVerStrategyState state) {
        def branchName = removeDisallowedChars(shortenBranch(state.currentBranch.name))
        def inferred = state.inferredPreRelease ? "${state.inferredPreRelease}.${branchName}" : "${branchName}"
        return state.copyWith(inferredPreRelease: inferred)
    }

    private String shortenBranch(String branchName) {
        replacePatterns.inject(branchName) { shortenedName, pattern, replacement ->
            shortenedName.replaceAll pattern, replacement
        }
    }

    private String removeDisallowedChars(String branchName) {
        branchName.replaceAll(~/[^0-9A-Za-z-]/, '-')
    }

}
