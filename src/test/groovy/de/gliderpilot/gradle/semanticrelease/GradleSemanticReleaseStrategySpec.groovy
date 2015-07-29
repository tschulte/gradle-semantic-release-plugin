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
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import spock.lang.Specification
import spock.lang.Subject

class GradleSemanticReleaseStrategySpec extends Specification {

	Grgit grgit = Mock()

	@Subject
	GradleSemanticReleaseStrategy strategy = new GradleSemanticReleaseStrategy(grgit, new GradleSemanticReleaseCommitMessageConventions())

	def "the initial version is 1.0.0"() {
		given:
		def initialState = initialState()

		when:
		def inferredState = strategy.infer(initialState)

		then:
		inferredState == initialState.copyWith(inferredNormal: '1.0.0')
		0 * grgit._
	}

	def "the initial version is 1.0.0 when the nearestVersion is less than 1.0.0"() {
		given:
		def initialState = initialState("0.1.0")

		when:
		def inferredState = strategy.infer(initialState)

		then:
		inferredState == initialState.copyWith(inferredNormal: '1.0.0')
		0 * grgit._
	}

	def "the version is not changed if no commits since last version"() {
		given:
		def initialState = initialState("1.1.0", 0)

		when:
		def inferredState = strategy.infer(initialState)

		then:
		inferredState == initialState
		0 * grgit._
	}

	def "patch version is incremented if no feature commits are found"() {
		given:
		def initialState = initialState("1.2.3", 1)

		when:
		def inferredState = strategy.infer(initialState)

		then:
		inferredState == initialState.copyWith(inferredNormal: "1.2.4")
		1 * grgit.methodMissing("log", _) >> [new Commit(shortMessage: 'shortMessage', fullMessage: 'shortMessage\n\ndescription')]
	}

	def "minor version is incremented if feature commits are found"() {
		given:
		def initialState = initialState("1.2.3", 1)

		when:
		def inferredState = strategy.infer(initialState)

		then:
		inferredState == initialState.copyWith(inferredNormal: "1.3.0")
		1 * grgit.methodMissing("log", _) >> [new Commit(shortMessage: 'feat: foo', fullMessage: 'feat: foo\n\ndescription')]
	}

	def "major version is incremented if breaking change commits are found"() {
		given:
		def initialState = initialState("1.2.3", 1)

		when:
		def inferredState = strategy.infer(initialState)

		then:
		inferredState == initialState.copyWith(inferredNormal: "2.0.0")
		1 * grgit.methodMissing("log", _) >> [new Commit(shortMessage: 'feat: foo', fullMessage: 'feat: foo\n\ndescription\n\nBREAKING CHANGE: foo')]
	}

	/*

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

	private SemVerStrategyState initialState(String previousVersion = null, int commitsSincePreviousVersion = 0) {
		new SemVerStrategyState(
				nearestVersion:
						previousVersion
								? new NearestVersion(normal: Version.valueOf(previousVersion), distanceFromNormal: commitsSincePreviousVersion)
								: null
		)
	}
}
