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
package de.gliderpilot.gradle.semanticrelease.integration

import spock.lang.Requires

// always run on travis
// also run on ./gradlew integTest
@Requires({ env['TRAVIS'] || properties['integTest'] })
class SemanticReleaseIntegrationAuxScriptSpec extends SemanticReleasePluginIntegrationSpec {

    @Override
    def setupGradleWrapper() {
        gradleVersion = '2.1'
        super.setupGradleWrapper()
    }

    @Override
    def setupGradleProject() {
        buildFile << '''
            apply from:'release.gradle'
            println version
        '''.stripIndent()
        def releaseScript = file('release.gradle')
        releaseScript << buildscript()
        releaseScript << """
            apply plugin: de.gliderpilot.gradle.semanticrelease.SemanticReleasePlugin
        """.stripIndent()
    }

}
