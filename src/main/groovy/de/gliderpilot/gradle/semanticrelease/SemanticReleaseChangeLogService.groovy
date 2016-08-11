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
import com.jcabi.github.*
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import groovy.transform.Memoized
import groovy.transform.PackageScope
import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.TagStrategy
import org.ajoberstar.gradle.git.release.semver.ChangeScope
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.regex.Matcher

/**
 * Created by tobias on 7/26/15.
 */
class SemanticReleaseChangeLogService {

    private final Logger logger = Logging.getLogger(getClass())

    private final TagStrategy tagStrategy
    private final Grgit grgit

    @PackageScope
    Github github
    private Closure<Iterable<File>> files
    private Iterable<File> releaseAssets

    SemanticReleaseChangeLogService(Grgit grgit, TagStrategy tagStrategy, Closure<Iterable<File>> files) {
        this.grgit = grgit
        this.tagStrategy = tagStrategy
        this.files = files
        releaseAssets = files()
    }

    void setGhToken(String token) {
        if (token)
            github = new RtGithub(token)
    }

    @Deprecated
    Github getGithub() {
        logger.warn("semanticRelease.changeLog.github is deprecated and will be removed in v2.0.0")
        github
    }

    @Deprecated
    void setGithub(Github github) {
        logger.warn("semanticRelease.changeLog.github is deprecated and will be removed in v2.0.0")
        this.github = github
    }

    /**
     * Parse the commits since the last release and return the change scope to use for
     * the next version. Return null, if no release is necessary, either because there
     * where no changes at all, or because there where no relevant changes.
     * If a Commit introduced relevant changes is determined using the changeScopeOfCommit
     * closure.
     *
     * @param commits the commits since the last release
     *
     * @return the change scope or null
     */
    @PackageScope
    ChangeScope changeScope(List<Commit> commits) {
        return commits.collect(changeScope).min()
    }

    /**
     * Closure to parse a commit and return the ChangeScope for that commit. Return null,
     * if this commit did not introduce a relevant change.
     */
    Closure<ChangeScope> changeScope = { Commit commit ->
        breaks(commit) ? ChangeScope.MAJOR
                : type(commit) == 'feat' ? ChangeScope.MINOR
                : type(commit) in ['fix', 'perf'] ? ChangeScope.PATCH
                : null
    }

    /**
     * Create a Writable that can be used to retrieve the change log.
     */
    Closure<Writable> changeLog = { List<Commit> commits, ReleaseVersion version ->
        String previousVersion = Version.valueOf(version.previousVersion).majorVersion ? version.previousVersion : null
        String previousTag = tagStrategy.toTagString(previousVersion)
        String currentTag = version.createTag ? tagStrategy.toTagString(version.version) : null
        Template template = new SimpleTemplateEngine().createTemplate(getClass().getResource('/CHANGELOG.md'))
        template.make([
                title     : null,
                versionUrl: versionUrl(previousTag, currentTag),
                service   : this,
                version   : version.version,
                fix       : byTypeGroupByComponent(commits, 'fix'),
                feat      : byTypeGroupByComponent(commits, 'feat'),
                perf      : byTypeGroupByComponent(commits, 'perf'),
                revert    : byTypeGroupByComponent(commits, 'revert'),
                breaks    : commits.collect(breaks).findAll { it },
                date      : new java.sql.Date(System.currentTimeMillis()).toString()
        ])
    }

    @PackageScope
    List<String> closesKeywords = ['Closes', 'Fixes']

    @PackageScope
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

    @PackageScope
    def breakingChangeKeywords = ['BREAKING CHANGE:']

    @PackageScope
    private def breaks = { Commit commit ->
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

    @PackageScope
    def type = { Commit commit ->
        def pattern = /(.*?)(?:\(.+\))?:.*/
        def matcher = commit.shortMessage =~ pattern
        matcher ? matcher.group(1).trim() : null
    }

    @PackageScope
    def component = { Commit commit ->
        def pattern = /.*?\((.+)\):.*/
        def matcher = commit.shortMessage =~ pattern
        matcher ? matcher.group(1).trim() : null
    }

    @PackageScope
    def subject = { Commit commit ->
        def pattern = /(?:.*?(?:\(.+\))?:)?(.*)/
        def matcher = commit.shortMessage =~ pattern
        matcher[0][1].trim()
    }

    @PackageScope
    def commitish = { Commit commit ->
        String url = repositoryUrl("commit/${commit.abbreviatedId}")
        url ? "[${commit.abbreviatedId}]($url)" : "${commit.abbreviatedId}"
    }

    @PackageScope
    def byTypeGroupByComponent = { List<Commit> commits, String typeFilter ->
        commits.findAll {
            type(it) == typeFilter
        }.groupBy(component).sort { a, b -> a.key <=> b.key }
    }

    @PackageScope
    @Memoized
    String mnemo() {
        String repositoryUrl = grgit.remote.list().find { it.name == 'origin' }.url
        Matcher matcher = repositoryUrl =~ /.*github.com[\/:]((?:.+?)\/(?:.+?))(?:\.git)/
        if (!matcher)
            return null
        return matcher.group(1)
    }

    @PackageScope
    Closure<String> repositoryUrl = { String suffix ->
        String mnemo = mnemo()
        if (!mnemo)
            return null
        return "https://github.com/${mnemo}/$suffix"
    }

    @PackageScope
    Closure<String> versionUrl = { String previousTag, String currentTag ->
        if (!(previousTag && currentTag))
            return null
        repositoryUrl("compare/${previousTag}...${currentTag}")
    }

    void releaseAssets(Object... assets) {
        releaseAssets += files(assets)
    }

    @PackageScope
    @Memoized
    List<Commit> commits(Version previousVersion) {
        grgit.log {
            includes << 'HEAD'
            if (previousVersion.majorVersion) {
                String previousVersionString = tagStrategy.toTagString(previousVersion.toString())
                // range previousVersionString, 'HEAD' does not work: https://github.com/ajoberstar/grgit/issues/71
                excludes << "${previousVersionString}^{commit}".toString()
            }
        }
    }

    @PackageScope
    void createGitHubVersion(ReleaseVersion version) {
        String mnemo = mnemo()
        if (!mnemo)
            return
        if (!github)
            return
        String tag = tagStrategy.toTagString(version.version)

        Repo repo = github.repos().get(new Coordinates.Simple(mnemo))

        // check for the existance of the tag using the api -> #3
        long start = System.currentTimeMillis()
        while (!tagExists(repo, tag) && System.currentTimeMillis() - start < 60000) {
        }

        Release release = repo.releases().create(tag)
        new Release.Smart(release).body(changeLog(commits(Version.valueOf(version.previousVersion)), version).toString())
        releaseAssets.each {
            release.assets().upload(it.bytes, URLConnection.guessContentTypeFromName(it.name) ?: "application/octet-stream", it.name)
        }
    }

    private boolean tagExists(Repo repo, String tag) {
        try {
            repo.git().references().get("refs/tags/$tag").json()
            return true
        } catch (Throwable t) {
            return false
        }
    }

}
