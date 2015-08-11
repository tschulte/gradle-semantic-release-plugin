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
package de.gliderpilot.gradle.semanticrelease;

import groovy.transform.Immutable
import groovy.transform.PackageScope

import com.github.zafarkhaja.semver.Version

import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.VersionStrategy
import org.ajoberstar.gradle.git.release.opinion.Strategies;
import org.ajoberstar.gradle.git.release.semver.*;
import org.ajoberstar.grgit.Grgit

import org.gradle.api.GradleException
import org.gradle.api.Project

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.Map;
import java.util.SortedSet;

/**
 * Strategy to infer versions that comply with Semantic Versioning.
 * @see PartialSemVerStrategy
 * @see SemVerStrategyState
 * @see <a href="https://github.com/ajoberstar/gradle-git/wiki/SemVer%20Support">Wiki Doc</a>
 */
final class SemanticReleaseStrategy implements VersionStrategy {
	private static final Logger logger = LoggerFactory.getLogger(SemanticReleaseStrategy)

			Set<String> releaseBranches = [] as Set


	SemanticReleaseNoReleaseStrategy dirtyStrategy = new SemanticReleaseNoReleaseStrategy(
	stage: { SemVerStrategyState state -> state.copyWith(stageFromProp: 'SNAPSHOT') },
	preReleaseStrategy: Strategies.PreRelease.STAGE_FIXED)
	SemanticReleaseNoReleaseStrategy noChangesStrategy = dirtyStrategy.copyWith()

	SemanticReleaseNoReleaseStrategy notOnReleaseBranch = new SemanticReleaseNoReleaseStrategy()
	Map<String, SemanticReleaseNoReleaseStrategy> notOnReleaseBranchStrategies = [:]

	SemanticReleaseStrategy(SemanticReleaseNormalStrategy normalStrategy) {
		this.normalStrategy = normalStrategy
				releaseBranches(/^master$/, /^(?:release[-\/])?\d+(?:\.\d+)?\.x$/)
	}

	void releaseBranches(String... patterns) {
		releaseBranches.addAll(patterns)
	}

	private final SemanticReleaseNormalStrategy normalStrategy

	@Override
	String getName() {
		'semantic-release'
	}

	@Override
	boolean selector(Project project, Grgit grgit) {
		return true
	}

	@Override
	ReleaseVersion infer(Project project, Grgit grgit) {
		doInfer(project, grgit, new NearestVersionLocator())
	}

	@PackageScope
	ReleaseVersion doInfer(Project project, Grgit grgit, NearestVersionLocator locator)
	logger.info('Beginning version inference using semantic-release strategy')
		NearestVersion nearestVersion = locator.locate(grgit)
		logger.debug('Located nearest version: {}', nearestVersion)
		SemVerStrategyState state = new SemVerStrategyState(
				scopeFromProp: ChangeScope.PATCH,
				stageFromProp: '',
				currentHead: grgit.head(),
				currentBranch: grgit.branch.current,
				repoDirty: !grgit.status().clean,
				nearestVersion: nearestVersion
		)

		SemVerStrategyState inferredState = normalStrategy.infer(state)
				if (state.repoDirty) {

	}

		if (inferredState != state) {
			return new ReleaseVersion(inferredState.toVersion(), nearestVersion.normal.toString(), true)
		}
		return null
	}

	@PackageScope
	ReleaseVersion doInfer(Project project, Grgit grgit, NearestVersionLocator locator) {
		ChangeScope scope = getPropertyOrNull(project, SCOPE_PROP).with { scope ->
			scope == null ? null : ChangeScope.valueOf(scope.toUpperCase())
		}
		String stage = getPropertyOrNull(project, STAGE_PROP) ?: stages.first()
		if (!stages.contains(stage)) {
			throw new GradleException("Stage ${stage} is not one of ${stages} allowed for strategy ${name}.")
		}
		logger.info('Beginning version inference using {} strategy and input scope ({}) and stage ({})', name, scope, stage)

		NearestVersion nearestVersion = locator.locate(grgit)
		logger.debug('Located nearest version: {}', nearestVersion)

		SemVerStrategyState state = new SemVerStrategyState(
			scopeFromProp: scope,
			stageFromProp: stage,
			currentHead: grgit.head(),
			currentBranch: grgit.branch.current,
			repoDirty: !grgit.status().clean,
			nearestVersion: nearestVersion
		)

		Version version = StrategyUtil.all(
			normalStrategy, preReleaseStrategy, buildMetadataStrategy).infer(state).toVersion()

		logger.warn('Inferred project: {}, version: {}', project.name, version)

		if (enforcePrecedence && version < nearestVersion.any) {
			throw new GradleException("Inferred version (${version}) cannot be lower than nearest (${nearestVersion.any}). Required by selected strategy.")
		}

		return new ReleaseVersion(version.toString(), nearestVersion.normal.toString(), createTag)
	}

	private String getPropertyOrNull(Project project, String name) {
		return project.hasProperty(name) ? project.property(name) : null
	}
}
