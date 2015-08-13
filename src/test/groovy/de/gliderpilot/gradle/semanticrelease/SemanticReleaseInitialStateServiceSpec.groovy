package de.gliderpilot.gradle.semanticrelease

import com.github.zafarkhaja.semver.Version
import groovy.mock.interceptor.MockFor
import org.ajoberstar.gradle.git.release.semver.NearestVersion
import org.ajoberstar.gradle.git.release.semver.NearestVersionLocator
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import org.ajoberstar.grgit.Branch
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.service.BranchService
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
