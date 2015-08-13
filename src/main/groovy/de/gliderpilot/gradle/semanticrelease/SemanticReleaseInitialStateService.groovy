package de.gliderpilot.gradle.semanticrelease

import groovy.transform.Memoized
import groovy.transform.PackageScope
import org.ajoberstar.gradle.git.release.semver.NearestVersion
import org.ajoberstar.gradle.git.release.semver.NearestVersionLocator
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by tobias on 8/12/15.
 */
class SemanticReleaseInitialStateService {
    private static final Logger logger = LoggerFactory.getLogger(SemanticReleaseInitialStateService)

    @Memoized
    SemVerStrategyState initialState(Project project, Grgit grgit) {
        return createInitialState(project, grgit, new NearestVersionLocator())
    }

    // for unit tests
    @PackageScope
    static SemVerStrategyState createInitialState(Project project, Grgit grgit, NearestVersionLocator locator) {
        NearestVersion nearestVersion = locator.locate(grgit)
        SemVerStrategyState initialState = new SemVerStrategyState(
                scopeFromProp: null,
                stageFromProp: null,
                currentHead: grgit.head(),
                currentBranch: grgit.branch.current,
                repoDirty: !grgit.status().clean,
                nearestVersion: nearestVersion
        )
        logger.info('Initial State: {}', initialState)
        return initialState
    }


}
