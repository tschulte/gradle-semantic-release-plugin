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
import groovy.transform.Immutable
import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.VersionStrategy
import org.ajoberstar.gradle.git.release.opinion.Strategies
import org.ajoberstar.gradle.git.release.semver.*
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Strategy to infer versions that comply with Semantic Versioning.
 *
 * @see PartialSemVerStrategy
 * @see SemVerStrategyState
 */
@Immutable(copyWith = true, knownImmutableClasses = [PartialSemVerStrategy, SemanticReleaseNormalStrategy, SemanticReleaseStrategySelector, SemanticReleaseInitialStateService])
final class SemanticReleaseStrategy implements VersionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SemanticReleaseStrategy)

    SemanticReleaseInitialStateService initialStateService

    SemanticReleaseStrategySelector selector = { false }

    SemanticReleaseNormalStrategy normalStrategy

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

    String type

    @Override
    String getName() {
        type ? "semantic-release-$type" : "semantic-release"
    }

    @Override
    boolean selector(Project project, Grgit grgit) {
        return selector.selector(initialStateService.initialState())
    }

    @Override
    ReleaseVersion infer(Project project, Grgit grgit) {
        logger.info("Beginning version inference using $name strategy")
        SemVerStrategyState initialState = initialStateService.initialState()

        Version version = StrategyUtil.all(
                StrategyUtil.one(normalStrategy,
                        enforceMajorIsGreaterThanZero(),
                        Strategies.Normal.useScope(ChangeScope.PATCH)),
                enforceReleaseBranchPattern(),
                preReleaseStrategy,
                buildMetadataStrategy).infer(initialState).toVersion()

        logger.warn('Inferred version: {}', version)

        return new ReleaseVersion(version.toString(), initialState.nearestVersion.normal.toString(), createTag)
    }

    private PartialSemVerStrategy enforceMajorIsGreaterThanZero() {
        return {
            it.nearestVersion.normal.majorVersion ? it : it.copyWith(inferredNormal: '1.0.0')
        } as PartialSemVerStrategy
    }

    /**
     * If the branch is of pattern /^(?:release[-\/])?\d+(?:\.\d+)?\.x$/, enforce that the version matches that pattern.
     * <p>
     * This means, if the semantic release normal strategy already increased the version to be out of range, throw an
     * exception. If the semantic release normal strategy did not already increased the version, increase it. So even if
     * there was no BREAKING CHANGE, the major version might be increased.
     *
     * @see Strategies.Normal.ENFORCE_BRANCH_MAJOR_MINOR_X
     * @see Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_MINOR_X
     * @see Strategies.Normal.ENFORCE_BRANCH_MAJOR_X
     * @see Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_X
     */
    private PartialSemVerStrategy enforceReleaseBranchPattern() {
        return { SemVerStrategyState oldState ->
            // We use the provided ENFORCE-Strategies of gradle-git. But these do only work with the nearestVersion.normal,
            // and we might already have changed the inferredNormal in the semantic release normal strategy.
            // Therefore we must create a tmpState first
            SemVerStrategyState tmpState = oldState.inferredNormal ?
                    oldState.copyWith(nearestVersion: new NearestVersion(normal: Version.valueOf(oldState.inferredNormal))) :
                    oldState
            SemVerStrategyState checkedState = StrategyUtil.all(Strategies.Normal.ENFORCE_BRANCH_MAJOR_MINOR_X,
                    Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_MINOR_X,
                    Strategies.Normal.ENFORCE_BRANCH_MAJOR_X,
                    Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_X).infer(tmpState)
            if (checkedState == tmpState) {
                // the ENFORCE-Strategies did not change anything, return the old state
                return oldState
            }
            // the ENFORCE-Strategies did change the state, but we must not directly return it, and instead merge the
            // changes into the oldState
            if (oldState.inferredNormal) {
                // the semantic release normal strategy did already change the version, only accept the inferred version
                // of the ENFORCE-Strategies, if the Major or Minor version differs
                Version oldVersion = Version.valueOf(oldState.inferredNormal)
                Version newVersion = Version.valueOf(checkedState.inferredNormal)
                if (oldVersion.majorVersion == newVersion.majorVersion && oldVersion.minorVersion == newVersion.minorVersion)
                    return oldState
            }
            return oldState.copyWith(inferredNormal: checkedState.inferredNormal)
        } as PartialSemVerStrategy
    }

}
