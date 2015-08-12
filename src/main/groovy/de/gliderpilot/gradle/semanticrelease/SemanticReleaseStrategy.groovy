/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import groovy.transform.PackageScope
import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.VersionStrategy
import org.ajoberstar.gradle.git.release.opinion.Strategies
import org.ajoberstar.gradle.git.release.semver.ChangeScope
import org.ajoberstar.gradle.git.release.semver.PartialSemVerStrategy
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import org.ajoberstar.gradle.git.release.semver.StrategyUtil
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

    SemanticReleaseInitialStateService initialStateService = new SemanticReleaseInitialStateService()

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

    @Override
    String getName() {
        'semantic-release'
    }

    @Override
    boolean selector(Project project, Grgit grgit) {
        return selector.selector(initialStateService.initialState(project, grgit))
    }

    @Override
    ReleaseVersion infer(Project project, Grgit grgit) {
        doInfer(initialStateService.initialState(project, grgit))
    }

    // for unit tests
    @PackageScope
    ReleaseVersion doInfer(SemVerStrategyState initialState) {

        logger.info('Beginning version inference using semantic-release strategy')

        Version version = StrategyUtil.all(
                StrategyUtil.one(normalStrategy,
                        {
                            it.nearestVersion.normal.majorVersion ? it : it.copyWith(inferredNormal: '1.0.0')
                        } as PartialSemVerStrategy,
                        Strategies.Normal.useScope(ChangeScope.PATCH)),
                preReleaseStrategy,
                buildMetadataStrategy).infer(initialState).toVersion()

        logger.warn('Inferred version: {}', version)

        return new ReleaseVersion(version.toString(), initialState.nearestVersion.normal.toString(), createTag)
    }

}
