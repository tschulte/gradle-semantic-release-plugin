package de.gliderpilot.gradle.semanticrelease

import groovy.transform.PackageScope
import org.ajoberstar.gradle.git.release.semver.PartialSemVerStrategy
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState

import java.util.regex.Pattern

/**
 * Created by tobias on 7/21/15.
 */
@PackageScope
class GradleSemanticReleaseAppendBranchNameStrategy implements PartialSemVerStrategy {

    def replacePatterns = [
            (~/^feature[-\/](.*)$/): '$1',
            (~/^master$/): '',
            (~/^(?:release[-\/])?\d+(?:\.\d+)?\.x$/): ''
    ]

    void replace(pattern, replacement) {
        replacePatterns[pattern] = replacement
    }

    @Override
    SemVerStrategyState infer(SemVerStrategyState state) {
        def branchName = removeDisallowedChars(shortenBranch(state.currentBranch.name))
        if (!branchName)
            return state

        def inferred = state.inferredPreRelease ? "${state.inferredPreRelease}.${branchName}" : "${branchName}"
        return state.copyWith(inferredPreRelease: inferred)
    }

    private String shortenBranch(String branchName) {
        replacePatterns.inject(branchName) { shortenedName, pattern, replacement ->
            shortenedName.replaceAll pattern, replacement
        }
    }

    private String removeDisallowedChars(String branchName) {
        branchName.replaceAll(~/[^0-9A-Za-z-]/, '')
    }

}
