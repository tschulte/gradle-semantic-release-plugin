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
        NearestVersionLocator locator = new NearestVersionLocator()
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
