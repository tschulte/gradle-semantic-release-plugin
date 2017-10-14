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

import org.ajoberstar.grgit.Grgit

import com.jcabi.github.RtGithub

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class GithubRepoSpec extends Specification {

    @Shared
    Grgit grgit = Grgit.open()

    @Shared
    @Subject
    GithubRepo repo

    def setup() {
        repo = new GithubRepo(grgit)
    }

    def "creates github service upon setting ghToken"() {
        when:
        repo.ghToken = '12345'

        then:
        repo.github instanceof RtGithub
        repo.github.entry().uri().get().toString().contains("github.com")
    }

    def "creates github enterprise service with api endpoint"() {
        when:
        repo.ghToken = '12345'
        repo.useGhEnterprise 'https://github.enterprise/'

        then:
        repo.github instanceof RtGithub
        repo.github.entry().uri().get().toString().equals("https://github.enterprise/api/v3")
    }

    def "creates github enterprise service with http authorization token upon setting ghToken and useGhEnterprise"() {
        when:
        repo.ghToken = '12345'
        repo.useGhEnterprise 'https://github.enterprise/'

        then:
        repo.github instanceof RtGithub
        repo.github.entry().toString().contains("Authorization: token 12345")
    }

    def "creates github enterprise service upon setting ghToken and useGhEnterprise"() {
        when:
        repo.useGhEnterprise 'https://github.enterprise/'

        then:
        repo.github instanceof RtGithub
        !repo.github.entry().toString().contains("Authorization: ")
    }

    @Unroll
    def "extract repository path for github.com and github enterprise from url"() {
        when:
        if (ghEnterprise) {
            repo.useGhEnterprise 'https://github.enterprise/'
        }

        then:
        repo.getPathFromRepositoryUrl(url) == expectedMnemo

        where:
        ghEnterprise | url                                                                    | expectedMnemo
        false        | "http://github.com/org/repo.git"                                       | "org/repo"
        true         | "https://anyurl/enterprise/repo.git"                                   | "enterprise/repo"
        false        | "git@github.com:tschulte/gradle-semantic-release-plugin.git"           | "tschulte/gradle-semantic-release-plugin"
        true         | "git@githubenterprise.com:tschulte/gradle-semantic-release-plugin.git" | "tschulte/gradle-semantic-release-plugin"
        false        | "git@githubenterprise.com:tschulte/gradle-semantic-release-plugin.git" | null
    }

    @Unroll
    def "diffUrl for #tag1 and #tag2 is #expectedUrl when setting ghBaseUrl"() {
        when:
        repo.useGhEnterprise 'https://github.enterprise/'
        String diffUrl = repo.diffUrl(tag1, tag2)

        then:
        diffUrl == expectedUrl

        where:
        tag1     | tag2     | expectedUrl
        "v1.0.0" | "v1.1.0" | "https://github.enterprise/${repo.mnemo}/compare/v1.0.0...v1.1.0"
        "v1.0.0" | null     | null
        null     | "v1.1.0" | null
    }

    @Unroll
    def "diffUrl for #tag1 and #tag2 is #expectedUrl"() {
        when:
        String diffUrl = repo.diffUrl(tag1, tag2)

        then:
        diffUrl == expectedUrl

        where:
        tag1     | tag2     | expectedUrl
        "v1.0.0" | "v1.1.0" | "https://github.com/${repo.mnemo}/compare/v1.0.0...v1.1.0"
        "v1.0.0" | null     | null
        null     | "v1.1.0" | null
    }

    def "generates commitUrl"() {
        when:
        String commitUrl = repo.commitUrl("affe")

        then:
        commitUrl == "https://github.com/${repo.mnemo}/commit/affe"
    }

    def "commitUrl for null is null"() {
        expect:
        repo.commitUrl(null) == null
    }
}
