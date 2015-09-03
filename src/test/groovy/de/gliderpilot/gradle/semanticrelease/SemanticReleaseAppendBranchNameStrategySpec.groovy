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

import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import org.ajoberstar.grgit.Branch
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class SemanticReleaseAppendBranchNameStrategySpec extends Specification {

    SemanticReleaseCheckBranch onReleaseBranch = new SemanticReleaseCheckBranch()

    @Subject
    SemanticReleaseAppendBranchNameStrategy strategy = new SemanticReleaseAppendBranchNameStrategy(onReleaseBranch)

    @Unroll
    def "an initial branchname #branchName leads to inferredPreRelease #inferredPreRelease"() {
        given:
        def initialState = initialState(branchName)

        when:
        def newState = strategy.infer(initialState)

        then:
        newState.inferredPreRelease == inferredPreRelease

        where:
        branchName                     | inferredPreRelease
        'develop'                      | 'develop'
        'feature'                      | 'feature'
        'feature/foo'                  | 'foo'
        'feature/#123-foo-bar_baz#'    | '-123-foo-bar-baz-'
        'feature/$#123-foo-bar_baz###' | '--123-foo-bar-baz---'
        'master'                       | null
    }

    def "the branch name is appended to an existing inferredPreRelease"() {
        given:
        def initialState = initialState('develop').copyWith(inferredPreRelease: 'foo')

        when:
        def newState = strategy.infer(initialState)

        then:
        newState.inferredPreRelease == 'foo.develop'
    }

    def initialState(String branchName) {
        new SemVerStrategyState(currentBranch: new Branch(fullName: branchName))
    }
}
