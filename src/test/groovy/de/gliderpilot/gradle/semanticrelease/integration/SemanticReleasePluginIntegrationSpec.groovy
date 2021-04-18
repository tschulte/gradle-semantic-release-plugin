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
import org.apache.commons.exec.util.StringUtils
import spock.lang.Requires
import spock.lang.Unroll

import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean

/**
 * Created by tobias on 7/2/15.
 */
// always run on travis
// also run on ./gradlew integTest
@Requires({ env['TRAVIS'] || properties['integTest'] })
class SemanticReleasePluginIntegrationSpec extends IntegrationSpec {

    final static gradleVersions = ["2.0", "3.0", "4.0", "5.0", "6.0", "7.0"]

    def setupTestProject(String gradleVersion) {
        this.gradleVersion = gradleVersion
        setupJvmArguments()
        setupGit()
        // create the gradle wrapper before the project is setup
        setupGradleWrapper()
        setupGradleProject()
        setupGitignore()
        commit('initial project layout')
        push()
    }

    def setupJvmArguments() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        String buildDir = new File('build').canonicalPath
        if (isWindows()) buildDir = buildDir.replace('\\', '/')
        jvmArguments = runtimeMxBean.getInputArguments().collect { it.replaceAll(/([:=])build/, '$1' + buildDir) }
        file('gradle.properties') << "org.gradle.jvmargs=${jvmArguments.join(' ')}"
    }

    /**
     * Gradle-git seems to have issues when run using the nebula integration spec.
     * This results in the integration spec to publish to the real repo -- even creating tags there.
     * Therefore we must not use the runTasks methods. This is the first and only time, we use it --
     * to create the wrapper (and the .gradle-test-kit/init.gradle script).
     */
    def setupGradleWrapper() {
        if (gradleVersion == '7.0' || gradleVersion == '2.0') {
            // workaround: nebula-test does not work with gradle 7.0 and 2.0
            // initialize the wrapper with 6.0 first
            def requestedVersion = gradleVersion
            gradleVersion = '6.0'
            runTasksSuccessfully(':wrapper')
            // remove spockframework jar from init.gradle
            def initGradle = file('.gradle-test-kit/init.gradle')
            def lines = initGradle.readLines().findAll { !it.contains('spockframework') }
            initGradle.text = lines.join(System.getProperty("line.separator"))
            // and now use the wrapper to upgrade to the requested version
            gradleVersion = requestedVersion
            gradlew 'wrapper', '--gradle-version', gradleVersion
            return
        }
        runTasksSuccessfully(':wrapper')
    }

    def setupGitignore() {
        file('.gitignore') << '''\
            .gradle/
            build/
            cobertura.ser
            '''.stripIndent()
    }

    def setupGradleProject() {
        buildFile << """
            apply plugin: 'de.gliderpilot.semantic-release'
            println version
            """.stripIndent()
    }

    def setupGit() {
        // create remote repository
        File origin = new File(projectDir, "../${projectDir.name}.git").canonicalFile
        origin.mkdir()
        execute origin, 'git', 'init', '--bare'

        // create workspace
        execute 'git', 'init'
        execute 'git', 'config', '--local', 'user.name', "Me"
        execute 'git', 'config', '--local', 'user.email', "me@example.com"


        execute 'git', 'remote', 'add', 'origin', "$origin"
        commit 'initial commit'
        // just in case, branch name might be main in newer git versions
        execute 'git', 'branch', '-m', 'master'
        push()
    }

    @Unroll
    def "[gradle #gv] initial version is 1.0.0 after feat commit"() {
        setupTestProject(gv)

        when:
        "a commit with type feat is made"
        commit("feat: foo")

        then:
        release() == 'v1.0.0'

        where:
        gv << gradleVersions
    }

    @Unroll
    def "[gradle #gv] initial version is 1.0.0 after fix commit"() {
        setupTestProject(gv)

        when:
        "a commit with type fix is made"
        commit("fix: foo")

        then:
        release() == 'v1.0.0'

        where:
        gv << gradleVersions
    }

    @Unroll
    def "[gradle #gv] initial version is 1.0.0 even after breaking change"() {
        setupTestProject(gv)

        when: "a breaking change commit is made without prior version"
        commit("feat: foo\n\nBREAKING CHANGE: bar")

        then:
        release() == 'v1.0.0'

        where:
        gv << gradleVersions
    }

    @Unroll
    def "[gradle #gv] complete lifecycle"() {
        setupTestProject(gv)

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

        where:
        gv << gradleVersions
    }

    @Unroll
    def "[gradle #gv] supports git flow (travis can execute ./gradlew release on all branches)"() {
        setupTestProject(gv)

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

        where:
        gv << gradleVersions
    }

    @Unroll
    def "[gradle #gv] supports release/MAJOR_X"() {
        setupTestProject(gv)

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

        where:
        gv << gradleVersions
    }

    @Unroll
    def "[gradle #gv] supports release/MAJOR_MINOR_X"() {
        setupTestProject(gv)

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

        where:
        gv << gradleVersions
    }

    def execute(File dir = projectDir, String... args) {
        def argsString = args.collect { StringUtils.quoteArgument(it) }.join(' ')
        println "========"
        println "executing $argsString"
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
            throw new RuntimeException("failed to execute $argsString")
        return lastLine
    }

    def release() {
        gradlew 'release', '--info', '--stacktrace'
        lastVersion()
    }

    def gradlew(String... args) {
        execute(["${isWindows() ? 'gradlew.bat' : './gradlew'}", '-I', '.gradle-test-kit/init.gradle', Arrays.asList(args)].flatten() as String[])
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
