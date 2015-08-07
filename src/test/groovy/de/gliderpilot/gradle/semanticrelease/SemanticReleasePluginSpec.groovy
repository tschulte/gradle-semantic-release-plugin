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

import nebula.test.ProjectSpec

/**
 * Created by tobias on 7/2/15.
 */
class SemanticReleasePluginSpec extends ProjectSpec {

    private static final String PLUGIN = 'de.gliderpilot.semantic-release'
    private static final String BASE_PLUGIN = 'de.gliderpilot.semantic-release-base'

    def 'apply does not throw exceptions'() {
        when:
        project.apply plugin: pluginName

        then:
        noExceptionThrown()

        where:
        pluginName << [PLUGIN, BASE_PLUGIN]
    }

    def 'apply is idempotent'() {
        when:
        project.apply plugin: pluginName
        project.apply plugin: pluginName

        then:
        noExceptionThrown()

        where:
        pluginName << [PLUGIN, BASE_PLUGIN]
    }

    def "can configure the commitMessageConventionStrategy"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease {
                commitMessages {
                    breakingChangeKeywords = ['breaks']
                }
            }
        }

        then:
        project.semanticRelease.commitMessageConventions.breakingChangeKeywords == ['breaks']
    }

    def "can configure the release branches"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease {
                releaseBranches {
                    exclude 'foo'
                }
            }
        }

        then:
        project.semanticRelease.onReleaseBranch.excludes == ['foo'] as Set
    }

    def "can configure the appendBranchNames strategy"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease {
                appendBranchNames {
                    replace 'foo', 'bar'
                }
            }
        }

        then:
        project.semanticRelease.appendBranchName.replacePatterns.foo == 'bar'
    }

}
