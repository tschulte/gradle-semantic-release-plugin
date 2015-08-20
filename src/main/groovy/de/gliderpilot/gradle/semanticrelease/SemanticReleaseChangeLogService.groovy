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

import org.ajoberstar.gradle.git.release.semver.ChangeScope
import org.ajoberstar.grgit.Commit

/**
 * Created by tobias on 7/26/15.
 */
class SemanticReleaseChangeLogService {

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

    def changeScope = { Commit commit ->
        breaks(commit) ? ChangeScope.MAJOR
                : type(commit) == 'feat' ? ChangeScope.MINOR
                : type(commit) == 'fix' ? ChangeScope.PATCH
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
    ChangeScope changeScope(List<Commit> commits) {
        return commits.collect(changeScope).sort().find { it }
    }

    /**
     * Create a Writable that can be used to retrieve the change log.
     *
     * @param commits the commits since the last release
     */
    Writable changeLog(List<Commit> commits) {
        return null
    }
}
