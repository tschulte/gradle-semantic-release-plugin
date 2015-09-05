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

import com.github.zafarkhaja.semver.Version
import com.jcabi.github.Coordinates
import com.jcabi.github.Github
import com.jcabi.github.Release
import com.jcabi.github.RtGithub
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.transform.Memoized
import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.TagStrategy
import org.ajoberstar.gradle.git.release.semver.ChangeScope
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit

import java.util.regex.Matcher

/**
 * Created by tobias on 7/26/15.
 */
class SemanticReleaseChangeLogService {

    private final TagStrategy tagStrategy

    Github github

    void setGhToken(String token) {
        github = new RtGithub(token)
    }

    SemanticReleaseChangeLogService(TagStrategy tagStrategy) {
        this.tagStrategy = tagStrategy
    }

    List<String> closesKeywords = ['Closes', 'Fixes']

    def closes = { Commit commit ->
        def pattern = /(?:${closesKeywords.join('|')})(.*)/
        commit.fullMessage.readLines().collect { line ->
            def matcher = line =~ pattern
            if (matcher)
                matcher.group(1).split(',').collect { it.trim() }.findAll { it ==~ /#\d+/ }.collect { it - '#' }
            else
                []
        }.flatten() as SortedSet
    }

    def breakingChangeKeywords = ['BREAKING CHANGE:']
    def breaks = { Commit commit ->
        def pattern = /(?:${breakingChangeKeywords.join('|')})(.*)/
        commit.fullMessage.readLines().inject(new StringBuilder()) { result, line ->
            if (result) {
                result << "$line\n"
            } else {
                def matcher = line =~ pattern
                if (matcher)
                    result << "${matcher.group(1)}\n"
            }
            result
        }.toString().trim()
    }

    def type = { Commit commit ->
        def pattern = /(.*?)(?:\(.+\))?:.*/
        def matcher = commit.shortMessage =~ pattern
        matcher ? matcher.group(1) : null
    }

    def component = { Commit commit ->
        def pattern = /.*?\((.+)\):.*/
        def matcher = commit.shortMessage =~ pattern
        matcher ? matcher.group(1) : null
    }

    def subject = { Commit commit ->
        def pattern = /(?:.*?(?:\(.+\))?:)?(.*)/
        def matcher = commit.shortMessage =~ pattern
        matcher[0][1].trim()
    }

    def commitish = { Grgit grgit, Commit commit ->
        String url = repositoryUrl(grgit, "commit/${commit.abbreviatedId}")
        url ? "[${commit.abbreviatedId}]($url)" : "${commit.abbreviatedId}"
    }

    def byTypeGroupByComponent = { List<Commit> commits, String typeFilter ->
        commits.findAll {
            type(it) == typeFilter
        }.groupBy(component).sort { a, b -> a.key <=> b.key }
    }

    /**
     * Closure to parse a commit and return the ChangeScope for that commit
     */
    Closure<ChangeScope> changeScopeOfCommit = { Commit commit ->
        breaks(commit) ? ChangeScope.MAJOR
                : type(commit) == 'feat' ? ChangeScope.MINOR
                : type(commit) in ['fix', 'perf'] ? ChangeScope.PATCH
                : null
    }

    /**
     * Parse the commits since the last release and return the change scope to use for
     * the next version. Return null, if no release is necessary, either because there
     * where no changes at all, or because there where no relevant changes.
     *
     * @param commits the commits since the last release
     *
     * @return the change scope or null
     */
    Closure<ChangeScope> changeScope = { List<Commit> commits ->
        return commits.collect(changeScopeOfCommit).min()
    }

    Writable changeLogForVersion(Grgit grgit, ReleaseVersion version) {
        String previousVersion = Version.valueOf(version.previousVersion).majorVersion ? version.previousVersion : null
        String previousTag = (previousVersion && tagStrategy.prefixNameWithV) ? "v$previousVersion" : previousVersion
        String currentTag = tagStrategy.prefixNameWithV ? "v$version.version" : version.version
        changeLog(
                grgit,
                previousTag,
                currentTag,
                version.version,
                commits(grgit, Version.valueOf(version.previousVersion)))
    }

    /**
     * Create a Writable that can be used to retrieve the change log.
     *
     * @param commits the commits since the last release
     */
    Closure<Writable> changeLog = { Grgit grgit, String previousTag, String currentTag, String version, List<Commit> commits ->
        Template template = new SimpleTemplateEngine().createTemplate(getClass().getResource('/CHANGELOG.md'))
        template.make([
                grgit     : grgit,
                title     : null,
                versionUrl: versionUrl(grgit, previousTag, currentTag),
                service   : this,
                version   : version,
                fix       : byTypeGroupByComponent(commits, 'fix'),
                feat      : byTypeGroupByComponent(commits, 'feat'),
                perf      : byTypeGroupByComponent(commits, 'perf'),
                revert    : byTypeGroupByComponent(commits, 'revert'),
                breaks    : commits.collect(breaks).findAll { it },
                date      : new java.sql.Date(System.currentTimeMillis()).toString()
        ])
    }

    @Memoized
    String mnemo(Grgit grgit) {
        String repositoryUrl = grgit.remote.list().find { it.name == 'origin' }.url
        Matcher matcher = repositoryUrl =~ /.*github.com[\/:]((?:.+?)\/(?:.+?))(?:\.git)/
        if (!matcher)
            return null
        return matcher.group(1)
    }

    Closure<String> repositoryUrl = { Grgit grgit, String suffix ->
        String mnemo = mnemo(grgit)
        if (!mnemo)
            return null
        return "https://github.com/${mnemo}/$suffix"
    }

    Closure<String> versionUrl = { Grgit grgit, String previousTag, String currentTag ->
        if (!(previousTag && currentTag))
            return null
        repositoryUrl(grgit, "compare/${previousTag}...${currentTag}")
    }

    @Memoized
    List<Commit> commits(Grgit grgit, Version previousVersion) {
        grgit.log {
            includes << 'HEAD'
            if (previousVersion.majorVersion) {
                String previousVersionString = (tagStrategy.prefixNameWithV ? 'v' : '') + previousVersion.toString()
                // range previousVersionString, 'HEAD' does not work: https://github.com/ajoberstar/grgit/issues/71
                excludes << "${previousVersionString}^{commit}".toString()
            }
        }
    }

    void createGitHubVersion(Grgit grgit, ReleaseVersion version) {
        String mnemo = mnemo(grgit)
        if (!mnemo)
            return
        if (!github)
            return
        String tag = tagStrategy.prefixNameWithV ? "v$version.version" : "$version.version"
        Release release = github.repos().get(new Coordinates.Simple(mnemo)).releases().create(tag)
        new Release.Smart(release).body(changeLogForVersion(grgit, version).toString())
    }
}
