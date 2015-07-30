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
package de.gliderpilot.gradle.de.gliderpilot.gradle.semanticrelease

import nebula.test.IntegrationSpec
import nebula.test.ProjectSpec
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Requires

/**
 * Created by tobias on 7/2/15.
 */
// does not work on travis at the moment
@IgnoreIf({env['TRAVIS']})
class GradleSemanticReleasePluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        File origin = new File(projectDir, "../${projectDir.name}.git")
        origin.mkdir()
        execute origin, 'git', 'init', '--bare'
        buildFile << '''
            apply plugin: 'de.gliderpilot.semantic-release'
        '''
        file('.gitignore') << '''\
            .gradle-test-kit/
            .gradle/
            gradle/
            build/
        '''.stripIndent()

        runTasksSuccessfully(':wrapper')

        execute 'git', 'init'
        execute 'git', 'add', '.'
        execute 'git', 'commit', '-m', '"initial commit"'
        execute 'git', 'remote', 'add', 'origin', "$origin"
        execute 'git', 'push', 'origin', 'HEAD', '-u'
    }

    def "initial version is 1.0.0"() {
        given:
        execute './gradlew', '-I', '.gradle-test-kit/init.gradle', ':release', '-Prelease.stage=final'

        when:
        def version = execute "git", "describe"

        then:
        version == 'v1.0.0'
    }

    def execute(File dir = projectDir, String... args) {
        println "executing ${args.join(' ')}"
        String processOut = args.execute(null, dir).inputStream.text.trim()
        println processOut
        return processOut
    }
}
