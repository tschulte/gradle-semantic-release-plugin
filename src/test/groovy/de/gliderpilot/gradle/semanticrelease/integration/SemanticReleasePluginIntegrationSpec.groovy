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

import nebula.test.IntegrationSpec
import spock.lang.Requires
import spock.lang.Unroll

/**
 * Created by tobias on 7/2/15.
 */
// always run on travis
// also run on ./gradlew integTest
@Requires({ env['TRAVIS'] || properties['integTest'] })
class SemanticReleasePluginIntegrationSpec extends IntegrationSpec {

    def setup() {
        setupGit()
        // create the gradle wrapper before the project is setup
        setupGradleWrapper()
        setupGradleProject()
        setupGitignore()
        commit('initial project layout')
        push()
    }

    /**
     * Gradle-git seems to have issues when run using the nebula integration spec.
     * This results in the integration spec to publish to the real repo -- even creating tags there.
     * Therefore we must not use the runTasks methods. This is the first and only time, we use it --
     * to create the wrapper (and the .gradle-test-kit/init.gradle script).
     */
    def setupGradleWrapper() {
        runTasksSuccessfully(':wrapper')
    }

    /**
     * extract the buildscript block from the file .gradle-test-kit/init.gradle
     */
    def buildscript() {
        def lines = file('.gradle-test-kit/init.gradle').readLines()
        lines.remove(0)
        lines.remove(lines.size() - 1)
        lines.join(System.getProperty("line.separator")).stripIndent()
    }

    def setupGitignore() {
        file('.gitignore') << '''\
            .gradle-test-kit/
            .gradle/
            gradle/
            build/
            cobertura.ser
            '''.stripIndent()
    }

    def setupGradleProject() {
        buildFile << buildscript()
        buildFile << """
            apply plugin: 'de.gliderpilot.semantic-release'
            println version
            """.stripIndent()
    }

    def setupGit() {
        // create remote repository
        File origin = new File(projectDir, "../${projectDir.name}.git")
        origin.mkdir()
        execute origin, 'git', 'init', '--bare'

        // create workspace
        execute 'git', 'init'
        execute 'git', 'config', '--local', 'user.name', "Me"
        execute 'git', 'config', '--local', 'user.email', "me@example.com"


        execute 'git', 'remote', 'add', 'origin', "$origin"
        commit 'initial commit'
        push()
    }

    @Unroll
    def "initial version is 1.0.0 after #type commit"() {
        when:
        "a commit with type $type is made"
        commit("$type: foo")

        then:
        release() == 'v1.0.0'

        where:
        type << ['feat', 'fix']
    }

    def "initial version is 1.0.0 even after breaking change"() {
        when: "a breaking change commit is made without prior version"
        commit("feat: foo\n\nBREAKING CHANGE: bar")

        then:
        release() == 'v1.0.0'
    }

    def "complete lifecycle"() {
        expect: 'no initial release without feature commit'
        release() == ''

        when: 'initial version is 1.0.0 after feature commit'
        commit("feat: foo")

        then:
        release() == 'v1.0.0'

        and: 'no release, if no changes'
        release() == 'v1.0.0'

        when: 'unpushed but committed fix'
        commit('fix: some commit message')

        then: 'release is performed'
        release() == 'v1.0.1'

        when: 'feature commit'
        commit('feat: feature')
        push()

        then: 'new minor release'
        release() == 'v1.1.0'

        when: 'feature commit but dirty workspace'
        commit('feat: feature')
        file('README.md') << '.'

        then: 'no release'
        release() == 'v1.1.0'

        when: 'breaking change'
        commit '''\
            feat: Feature

            BREAKING CHANGE: everything changed
        '''.stripIndent()

        then: 'major release'
        release() == 'v2.0.0'

        when: 'empty commit message'
        commit('')
        push()

        then: 'no new version'
        release() == 'v2.0.0'
    }

    def "supports git flow (travis can execute ./gradlew release on all branches)"() {
        given: "branch develop"
        createBranch "develop"

        and: "new feature on feature branch foo"
        createBranch "feature/foo"

        when: "feature commit on this feature branch"
        commit("feat: foo")
        push()

        then: "no release"
        release() == ''

        when: "merging the feature branch into develop"
        checkout "develop"
        execute "git", "merge", "feature/foo"

        then: "no release either"
        release() == ''

        when: "releasing on release branch"
        createBranch "release/1.0.x"

        then: "release 1.0.0"
        release() == 'v1.0.0'

        when: "merge into master"
        checkout "master"
        execute "git", "merge", "release/1.0.x"

        then: "no new release"
        release() == 'v1.0.0'
    }

    def "supports release/MAJOR_X"() {
        given: "branch release/1.x"
        createBranch "release/1.x"
        push()

        when: "feature commit on this branch"
        commit("feat: foo")

        then: "release 1.0.0"
        release() == 'v1.0.0'

        when: "feature commit on this branch"
        commit("feat: foo")

        then: "release 1.1.0"
        release() == 'v1.1.0'

        when: "fix commit on this branch"
        commit("fix: foo")

        then: "release 1.1.1"
        release() == 'v1.1.1'

        when: "branch release/2.x"
        createBranch "release/2.x"

        then: "no release (no change)"
        release() == 'v1.1.1'

        when: "fix commit on this branch"
        commit("fix: foo")

        then: "release 2.0.0 (major bumb although only fix)"
        release() == 'v2.0.0'

        when: "breaking commit on this branch"
        commit("feat: foo\n\nBREAKING CHANGE: this breaks everything")

        and: "release is called"
        release()

        then: "no release, because branch disallows this"
        thrown(RuntimeException)
    }

    def "supports release/MAJOR_MINOR_X"() {
        given: "branch release/1.0.x"
        createBranch "release/1.0.x"
        push()

        when: "feature commit on this branch"
        commit("feat: foo")

        then: "release 1.0.0"
        release() == 'v1.0.0'

        when: "fix commit on this branch"
        commit("fix: foo")

        then: "release 1.0.1"
        release() == 'v1.0.1'

        when: "branch release/2.0.x"
        createBranch "release/2.0.x"

        then: "no release"
        release() == 'v1.0.1'

        when: "fix commit on this branch"
        commit("fix: foo")

        then: "release 2.0.0 (major bumb although only fix)"
        release() == 'v2.0.0'

        when: "feature commit on this branch"
        commit("feat: foo")

        and: "release is called"
        release()

        then: "no release, because branch disallows this"
        thrown(RuntimeException)
    }

    def execute(File dir = projectDir, String... args) {
        println "========"
        println "executing ${args.join(' ')}"
        println "--------"
        def lastLine
        def process = new ProcessBuilder(args)
                .directory(dir)
                .redirectErrorStream(true)
                .start()
        process.inputStream.eachLine {
            println it
            lastLine = it
        }
        def exitValue = process.waitFor()
        if (exitValue != 0)
            throw new RuntimeException("failed to execute ${args.join(' ')}")
        return lastLine
    }

    def release() {
        execute "${isWindows() ? 'gradlew.bat' : './gradlew'}", 'release', '--info', '--stacktrace'
        lastVersion()
    }

    def lastVersion() {
        try {
            execute "git", "describe", "--abbrev=0"
        } catch (any) {
            // ignore
            return ""
        }
    }

    def commit(message) {
        execute 'git', 'add', '.'
        execute 'git', 'commit', '--allow-empty', '--allow-empty-message', '-m', message ?: "''"
    }

    def push() {
        execute 'git', 'push', 'origin', 'HEAD', '-u'
    }

    def createBranch(String branch) {
        execute "git", "checkout", "-b", branch
        push()
    }

    def checkout(String branch) {
        execute "git", "checkout", branch
    }

    boolean isWindows() {
        System.properties['os.name'].toLowerCase().contains('windows')
    }

}
