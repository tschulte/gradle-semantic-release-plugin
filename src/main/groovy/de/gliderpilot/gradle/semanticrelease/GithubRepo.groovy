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

import com.jcabi.github.Github
import com.jcabi.github.RtGithub
import groovy.transform.Memoized
import groovy.transform.PackageScope
import org.ajoberstar.grgit.Grgit

import java.util.regex.Matcher

class GithubRepo extends GitRepo {

    private final Grgit grgit

    private Github github

    @PackageScope
    Github getGithub() {
        github
    }

    GithubRepo(Grgit grgit) {
        this.grgit = grgit
    }

    void setGhToken(String token) {
        if (token)
            github = new RtGithub(token)
    }

    @PackageScope
    @Memoized
    String getMnemo() {
        String repositoryUrl = grgit.remote.list().find { it.name == 'origin' }.url
        Matcher matcher = repositoryUrl =~ /.*github.com[\/:]((?:.+?)\/(?:.+?))(?:\.git)/
        if (!matcher)
            return null
        return matcher.group(1)
    }

    private String repositoryUrl(String suffix) {
        if (!mnemo)
            return null
        return "https://github.com/${mnemo}/$suffix"
    }

    String diffUrl(String previousTag, String currentTag) {
        if (!(previousTag && currentTag))
            return null
        repositoryUrl("compare/${previousTag}...${currentTag}")
    }

    String commitUrl(String abbreviatedId) {
        if (!abbreviatedId)
            return null
        repositoryUrl("commit/${abbreviatedId}")
    }

}