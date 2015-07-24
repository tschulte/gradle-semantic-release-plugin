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
import org.ajoberstar.gradle.git.release.semver.NearestVersion
import org.ajoberstar.gradle.git.release.semver.SemVerStrategyState
import spock.lang.Specification
import spock.lang.Subject

class GradleSemanticReleaseStrategySpec extends Specification {

	SemVerStrategyState initialState = new SemVerStrategyState()

	@Subject
	GradleSemanticReleaseStrategy strategy = new GradleSemanticReleaseStrategy()

	def "the initial version is 1.0.0"() {
		expect:
		strategy.infer(initialState) == initialState.copyWith(inferredNormal: '1.0.0')
	}

	def "the initial version is 1.0.0 when the nearestVersion is less than 1.0.0"() {
		when:
		initialState = initialState.copyWith(nearestVersion: new NearestVersion(normal: Version.valueOf("0.1.0")))
		then:
		strategy.infer(initialState) == initialState.copyWith(inferredNormal: '1.0.0')
	}

	def "the version is not changed if no commits since last version"() {
		when:
		initialState = initialState.copyWith(nearestVersion: new NearestVersion(normal: Version.valueOf("1.1.0"), distanceFromNormal: 0))
		then:
		strategy.infer(initialState) == initialState
	}

/*
	def "minor version is incremented if no feature commits are found"() {
		given:
		when:
		initialState = initialState.copyWith(nearestVersion: new NearestVersion(normal: Version.valueOf("1.2.3")))

		then:

	}

	def "integTest"() {
		def project = mockProject(null, null)
		def grgit = mockGrgit(false)
		def locator = mockLocator(null, null)
		def semVerStrategy = Strategies.FINAL.copyWith(normalStrategy: strategy)
		expect:
		semVerStrategy.doInfer(project, grgit, locator).version == "1.0.0"
	}
	def mockProject(Grgit grgit) {
		Project project = Mock()

		project.hasProperty('release.scope') >> (scope as boolean)
		project.property('release.scope') >> scope

		project.hasProperty('release.stage') >> (stage as boolean)
		project.property('release.stage') >> stage

		return project
	}

	def mockGrgit(boolean repoDirty, String branchName = 'master') {
		Grgit grgit = GroovyMock()

		Status status = Mock()
		status.clean >> !repoDirty
		grgit.status() >> status

		grgit.head() >> new Commit(id: '5e9b2a1e98b5670a90a9ed382a35f0d706d5736c')

		BranchService branch = GroovyMock()
		branch.current >> new Branch(fullName: "refs/heads/${branchName}")
		grgit.branch >> branch

		return grgit
	}

	def mockLocator(String nearestNormal, String nearestAny) {
		NearestVersionLocator locator = Mock()
		locator.locate(_) >> new NearestVersion(
			normal: nearestNormal ? Version.valueOf(nearestNormal) : null,
			distanceFromNormal: 5,
			any: nearestAny ? Version.valueOf(nearestAny) : null,
			distanceFromAny: 2
		)
		return locator
	}
	*/
}
