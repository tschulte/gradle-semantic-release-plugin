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

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import com.jcabi.http.request.ApacheRequest
import com.jcabi.http.wire.AutoRedirectingWire

import java.util.regex.Matcher

class GithubRepo extends GitRepo {

    private final Grgit grgit

    private Github github

    private String ghBaseUrl

    private boolean isGhEnterprise

    private String ghToken

    @PackageScope
    Github getGithub() {
        github
    }

    GithubRepo(Grgit grgit) {
        this.ghBaseUrl = "https://github.com"
        this.grgit = grgit
        this.isGhEnterprise = false
    }

    void setGhToken(String githubToken) {
        this.ghToken = githubToken
        this.github = buildGithubReference()
    }

    public String getGhBaseUrl() {
        return this.ghBaseUrl
    }

    public void useGhEnterprise(String ghEnterpriseUrl) {
        this.ghBaseUrl = ghEnterpriseUrl.replaceAll("/+\$", "") // remove trailing slashes
        this.isGhEnterprise = true
        this.github = buildGithubReference()
    }

    @PackageScope
    @Memoized
    String getMnemo() {
        String repositoryUrl = grgit.remote.list().find { it.name == 'origin' }.url
        Matcher matcher = repositoryUrl =~ /.*[\/:](.*\/.*)(?:\.git)$/
        if (!matcher)
            return null
        return matcher.group(1)
    }

    private RtGithub buildGithubReference() {
        if (this.isGhEnterprise) {
            // for github enterprise repositories
            def request = new ApacheRequest("${ghBaseUrl}/api/v3")
                    .header(HttpHeaders.USER_AGENT, RtGithub.USER_AGENT)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)

            if (this.ghToken != null) { // also add authentication token, if available
                request = request.header(HttpHeaders.AUTHORIZATION, "token ${ghToken}")
            }

            request = request.through(AutoRedirectingWire.class)

            return new RtGithub(request)
        } else if (this.ghToken) { // for github.com repositories
            return new RtGithub(this.ghToken)
        }
    }

    private String repositoryUrl(String suffix) {
        if (!mnemo)
            return null
        return "${ghBaseUrl}/${mnemo}/$suffix"
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
