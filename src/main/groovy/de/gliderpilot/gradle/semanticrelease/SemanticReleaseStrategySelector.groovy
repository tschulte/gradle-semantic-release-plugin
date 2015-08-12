package de.gliderpilot.gradle.semanticrelease

import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState

/**
 * Created by tobias on 8/11/15.
 */
interface SemanticReleaseStrategySelector {

    boolean selector(SemVerStrategyState initialState)

}