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

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class SemanticReleaseCheckBranchSpec extends Specification {

    @Subject
    SemanticReleaseCheckBranch strategy = new SemanticReleaseCheckBranch()

    @Unroll
    def "#branchName #description"() {
        expect:
        strategy.isReleaseBranch(branchName) == isReleaseBranch

        where:
        branchName             | isReleaseBranch
        'master'               | true
        'release/1.2.x'        | true
        '1.2.x'                | true
        'release-1.2.x'        | true
        '1.x'                  | true
        'release/1.x'          | true
        'release-1.x'          | true
        'develop'              | false
        'feature/#123-foo-bar' | false
        'dev-foo-bar'          | false

        description = isReleaseBranch ? 'is a release branch' : 'is no release branch'
    }

    def "can work with a blacklist instead of a whitelist"() {
        given:
        strategy.includes.clear()
        strategy.exclude('develop/.*')

        expect:
        strategy.isReleaseBranch('master')

        and:
        !strategy.isReleaseBranch('develop/foo')
    }

}
