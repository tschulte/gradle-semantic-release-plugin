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
