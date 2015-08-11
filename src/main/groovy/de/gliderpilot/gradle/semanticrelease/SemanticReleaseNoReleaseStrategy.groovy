package de.gliderpilot.gradle.semanticrelease

import groovy.transform.Immutable
import org.ajoberstar.gradle.git.release.opinion.Strategies
import org.ajoberstar.gradle.git.release.semver.PartialSemVerStrategy
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState

/**
 * Partial strategies to apply when not doing a release.
 */
@Immutable(copyWith=true, knownImmutableClasses=[PartialSemVerStrategy])
class SemanticReleaseNoReleaseStrategy {

    /**
     * The strategy used to infer the stage to use. Using this, the default Strategies can be used.
     */
    PartialSemVerStrategy stageStrategy = { SemVerStrategyState state -> state }

    /**
     * The strategy used to infer the pre-release component of the version. There is no enforcement that
     * this strategy only modify that part of the state.
     */
    PartialSemVerStrategy preReleaseStrategy = Strategies.PreRelease.NONE

    /**
     * The strategy used to infer the build metadata component of the version. There is no enforcement that
     * this strategy only modify that part of the state.
     */
    PartialSemVerStrategy buildMetadataStrategy = Strategies.BuildMetadata.NONE

    /**
     * Whether or not to create tags for versions inferred by this strategy.
     */
    boolean createTag = false
}
