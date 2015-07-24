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
import org.ajoberstar.gradle.git.release.opinion.Strategies
import org.ajoberstar.gradle.git.release.semver.NearestVersion
import org.ajoberstar.gradle.git.release.semver.NearestVersionLocator
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import org.ajoberstar.grgit.Branch
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Status
import org.ajoberstar.grgit.service.BranchService
import org.gradle.api.GradleException
import org.gradle.api.Project
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class GradleSemanticReleaseCheckReleaseBranchStrategySpec extends Specification {

	@Subject
	GradleSemanticReleaseCheckReleaseBranchStrategy strategy = new GradleSemanticReleaseCheckReleaseBranchStrategy()

	@Unroll
	def "the initial state is not changed for branch #branchName"() {
		given:
		def initialState = initialState(branchName)
		when:
		def newState = strategy.infer(initialState)

		then:
		noExceptionThrown()
		newState == initialState

		where:
		branchName << ['master', 'release/1.2.x', '1.2.x', 'release-1.2.x', '1.x', 'release/1.x', 'release-1.x']
	}

	@Unroll
	def "a GradleException is thrown for branch #branchName"() {
		when:
		strategy.infer(initialState(branchName))

		then:
		thrown(GradleException)

		where:
		branchName << ['develop', 'feature/#123-foo-bar', 'dev-foo-bar']
	}

	def initialState(String branchName) {
		new SemVerStrategyState(currentBranch: new Branch(fullName: branchName))
	}
}
