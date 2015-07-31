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
        // create remote repository
        File origin = new File(projectDir, "../${projectDir.name}.git")
        origin.mkdir()
        execute origin, 'git', 'init', '--bare'

        // create workspace
        execute 'git', 'init'
        execute 'git', 'remote', 'add', 'origin', "$origin"
        commit 'initial commit'
        execute 'git', 'push', 'origin', 'HEAD', '-u'

        buildFile << '''
            apply plugin: 'de.gliderpilot.semantic-release'
            println version
        '''
        file('.gitignore') << '''\
            .gradle-test-kit/
            .gradle/
            gradle/
            build/
        '''.stripIndent()

        file('README.md')

        runTasksSuccessfully(':wrapper')

        commit('initial project layout')
        push()
    }

    def "complete lifecycle"() {
        expect: 'initial version is 1.0.0'
        release() == 'v1.0.0'

        and: 'no release, if no changes'
        release() == 'v1.0.0'

        when: 'unpushed but committed changes'
        file('README.md') << '.'
        commit('some commit message')

        then: 'release is performed'
        release() == 'v1.0.1'
    }

    def execute(File dir = projectDir, String... args) {
        println "========"
        println "executing ${args.join(' ')}"
        println "--------"
        def process = args.execute(null, dir)
        String processOut = process.inputStream.text.trim()
        println processOut
        println process.errorStream.text
        return processOut
    }

    def release() {
        execute './gradlew', '-I', '.gradle-test-kit/init.gradle', ':release', '-Prelease.stage=final', '--info', '--stacktrace'
        execute "git", "describe"
    }

    def commit(message) {
        execute 'git', 'add', '.'
        execute 'git', 'commit', '--allow-empty', '-m', message
    }

    def push() {
        execute 'git', 'push'
    }
}
