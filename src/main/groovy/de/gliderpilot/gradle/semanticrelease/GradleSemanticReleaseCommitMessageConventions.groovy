package de.gliderpilot.gradle.semanticrelease

import org.ajoberstar.grgit.Commit

/**
 * Created by tobias on 7/26/15.
 */
class GradleSemanticReleaseCommitMessageConventions {

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

}
