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
import org.ajoberstar.gradle.git.release.semver.NearestVersion
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class SemanticReleaseNormalStrategySpec extends Specification {

    Grgit grgit = Mock()

    TagStrategy tagStrategy = new TagStrategy()
    SemanticReleaseChangeLogService changeLogService = new SemanticReleaseChangeLogService(tagStrategy)

    @Subject
    SemanticReleaseNormalStrategy strategy = new SemanticReleaseNormalStrategy(grgit,
            changeLogService)

    def "no initial version if no feature commit"() {
        given:
        def initialState = initialState()

        when:
        def inferredState = strategy.infer(initialState)
        def canRelease = strategy.canRelease(initialState)

        then:
        !canRelease
        inferredState == initialState
        1 * grgit.methodMissing("log", _) >> [commit('shortMessage')]
    }

    def "the initial version is 1.0.0 if there is a feature commit"() {
        given:
        def initialState = initialState()

        when:
        def inferredState = strategy.infer(initialState)
        def canRelease = strategy.canRelease(initialState)

        then:
        canRelease
        inferredState == initialState.copyWith(inferredNormal: '1.0.0')
        1 * grgit.methodMissing("log", _) >> [commit('feat: desrciption')]
    }

    def "no version when the nearestVersion is less than 1.0.0 and no feature commit"() {
        given:
        def initialState = initialState("0.1.0")

        when:
        def inferredState = strategy.infer(initialState)
        def canRelease = strategy.canRelease(initialState)

        then:
        !canRelease
        inferredState == initialState
        1 * grgit.methodMissing("log", _) >> [commit('shortMessage')]
    }

    def "the initial version is 1.0.0 when the nearestVersion is less than 1.0.0 and there is a feature commit"() {
        given:
        def initialState = initialState("0.1.0")

        when:
        def inferredState = strategy.infer(initialState)
        def canRelease = strategy.canRelease(initialState)

        then:
        canRelease
        inferredState == initialState.copyWith(inferredNormal: '1.0.0')
        1 * grgit.methodMissing("log", _) >> [commit('feat: desrciption')]
    }

    def "the version is not changed if no commits since last version"() {
        given:
        def initialState = initialState("1.1.0", 0)

        when:
        def inferredState = strategy.infer(initialState)
        def canRelease = strategy.canRelease(initialState)

        then:
        !canRelease
        inferredState == initialState
        0 * grgit._
    }

    @Unroll
    def "requests the log since #expectedSince (using the configuration from gradle-git) and HEAD"() {
        given:
        def initialState = initialState(initialVersion, 1)
        def since = []
        def until = []
        def logConfig = new Object() {
            def getIncludes() { until }

            def getExcludes() { since }
        }
        tagStrategy.prefixNameWithV = prefixNameWithV

        when:
        def inferredState = strategy.infer(initialState)

        then:
        1 * grgit.methodMissing("log", { it[0].delegate = logConfig; it[0](); true })
        since == expectedSince
        until == ['HEAD']

        where:
        prefixNameWithV | initialVersion | expectedSince
        false           | "1.2.3"        | ['1.2.3^{commit}']
        true            | "1.2.3"        | ['v1.2.3^{commit}']
        false           | "0.0.0"        | []
        true            | "0.0.0"        | []
    }

    def "version is not incremented if no feature nor fix commits are found"() {
        given:
        def initialState = initialState("1.2.3", 1)

        when:
        def inferredState = strategy.infer(initialState)
        def canRelease = strategy.canRelease(initialState)

        then:
        !canRelease
        inferredState == initialState
        1 * grgit.methodMissing("log", _) >> [commit('foo')]
    }


    def "patch version is incremented if no feature commits are found"() {
        given:
        def initialState = initialState("1.2.3", 1)

        when:
        def inferredState = strategy.infer(initialState)
        def canRelease = strategy.canRelease(initialState)

        then:
        canRelease
        inferredState == initialState.copyWith(inferredNormal: "1.2.4")
        1 * grgit.methodMissing("log", _) >> [commit('fix: foo')]
    }

    def "minor version is incremented if feature commits are found"() {
        given:
        def initialState = initialState("1.2.3", 1)

        when:
        def inferredState = strategy.infer(initialState)
        def canRelease = strategy.canRelease(initialState)

        then:
        canRelease
        inferredState == initialState.copyWith(inferredNormal: "1.3.0")
        1 * grgit.methodMissing("log", _) >> [commit('feat: foo')]
    }

    def "major version is incremented if breaking change commits are found"() {
        given:
        def initialState = initialState("1.2.3", 1)

        when:
        def inferredState = strategy.infer(initialState)
        def canRelease = strategy.canRelease(initialState)

        then:
        canRelease
        inferredState == initialState.copyWith(inferredNormal: "2.0.0")
        1 * grgit.methodMissing("log", _) >> [commit('feat: foo\n\ndescription\n\nBREAKING CHANGE: foo')]
    }

    private SemVerStrategyState initialState(String previousVersion = "0.0.0", int commitsSincePreviousVersion = 1) {
        new SemVerStrategyState(
                nearestVersion:
                        previousVersion
                                ? new NearestVersion(normal: Version.valueOf(previousVersion), distanceFromNormal: commitsSincePreviousVersion)
                                : null
        )
    }

    private Commit commit(String message) {
        new Commit(shortMessage: message.readLines().first(), fullMessage: message)
    }
}
