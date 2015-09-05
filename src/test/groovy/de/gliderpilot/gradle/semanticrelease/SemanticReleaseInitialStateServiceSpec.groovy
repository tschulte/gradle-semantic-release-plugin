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
import groovy.mock.interceptor.MockFor
import org.ajoberstar.gradle.git.release.semver.NearestVersion
import org.ajoberstar.gradle.git.release.semver.NearestVersionLocator
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import spock.lang.Specification
import spock.lang.Subject

/**
 * Created by tobias on 8/13/15.
 */
class SemanticReleaseInitialStateServiceSpec extends Specification {

    Project project = Mock()
    Grgit grgit = Grgit.open()

    @Subject
    SemanticReleaseInitialStateService service = new SemanticReleaseInitialStateService()

    def "retrieves initialState using NearestVersionLocator"() {
        given:
        NearestVersionLocator locator = Mock()
        def groovyMockFor = new MockFor(NearestVersionLocator)
        Version version = Version.valueOf('0.0.0')
        NearestVersion nearestVersion = new NearestVersion(version, version, 0, 0)
        groovyMockFor.demand.with {
            locate({ it == grgit }) { nearestVersion }
        }

        when:
        SemVerStrategyState initialState
        groovyMockFor.use {
            initialState = service.initialState(project, grgit)
        }

        then:
        initialState
        initialState.currentHead == grgit.head()
        initialState.currentBranch == grgit.branch.current
        initialState.nearestVersion == nearestVersion
        initialState.repoDirty != grgit.status().clean
        initialState.stageFromProp == null
        initialState.scopeFromProp == null
    }
}
