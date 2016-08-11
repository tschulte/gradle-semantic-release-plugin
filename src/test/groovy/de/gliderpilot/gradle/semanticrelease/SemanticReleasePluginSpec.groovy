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

    def 'apply does not throw exceptions'() {
        when:
        project.apply plugin: PLUGIN

        then:
        noExceptionThrown()
    }

    def 'apply is idempotent'() {
        when:
        project.apply plugin: PLUGIN
        project.apply plugin: PLUGIN

        then:
        noExceptionThrown()
    }

    def "can configure the changeLogService"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease {
                changeLog {
                    breakingChangeKeywords = ['breaks']
                }
            }
        }

        then:
        project.semanticRelease.changeLog.breakingChangeKeywords == ['breaks']
    }

    def "can configure the changeLogService using the property"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease.changeLog.breakingChangeKeywords = ['breaks']
        }

        then:
        project.semanticRelease.changeLog.breakingChangeKeywords == ['breaks']
    }

    def "can configure the changeLogService using the deprecated property"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease.changeLogService.breakingChangeKeywords = ['breaks']
        }

        then:
        project.semanticRelease.changeLog.breakingChangeKeywords == ['breaks']
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
        project.semanticRelease.releaseBranches.excludes == ['foo'] as Set
    }

    def "can configure the release branches using the property"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease.releaseBranches.exclude 'foo'
        }

        then:
        project.semanticRelease.releaseBranches.excludes == ['foo'] as Set
    }

    def "can configure the release branches using the deprecated property"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease.onReleaseBranch.exclude 'foo'
        }

        then:
        project.semanticRelease.releaseBranches.excludes == ['foo'] as Set
    }

    def "can configure the branchNames strategy"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease {
                branchNames {
                    replace 'foo', 'bar'
                }
            }
        }

        then:
        project.semanticRelease.branchNames.replacePatterns.foo == 'bar'
    }

    def "can configure the branchNames using the property"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease.branchNames.replace 'foo', 'bar'
        }

        then:
        project.semanticRelease.branchNames.replacePatterns.foo == 'bar'
    }

    def "can configure the branchNames using the deprecated property"() {
        when:
        project.with {
            apply plugin: PLUGIN
            semanticRelease.appendBranchName.replace 'foo', 'bar'
        }

        then:
        project.semanticRelease.branchNames.replacePatterns.foo == 'bar'
    }

    def "autowires release task dependencies"() {
        when:
        project.with {
            apply plugin: PLUGIN
            apply plugin: 'java'
            apply plugin: 'maven-publish'
        }

        then:
        project.tasks.release.dependsOn.contains(project.tasks.build)
        project.tasks.release.finalizedBy.getDependencies(project.tasks.release).contains(project.tasks.publish)
        project.tasks.release.finalizedBy.getDependencies(project.tasks.release).contains(project.tasks.uploadArchives)
    }

    def "can define releaseAssets"() {
        when:
        project.with {
            apply plugin: PLUGIN
            apply plugin: 'java'
            semanticRelease {
                changeLog {
                    releaseAssets jar
                }
            }
        }

        then:
        project.semanticRelease.changeLog.releaseAssets.toList().containsAll(project.jar.outputs.files.files)
    }
}
