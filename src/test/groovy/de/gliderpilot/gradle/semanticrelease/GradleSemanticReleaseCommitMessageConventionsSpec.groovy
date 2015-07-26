package de.gliderpilot.gradle.semanticrelease

import org.ajoberstar.grgit.Commit
import spock.lang.Specification
import spock.lang.Subject

/**
 * Created by tobias on 7/26/15.
 */
class GradleSemanticReleaseCommitMessageConventionsSpec extends Specification {

    @Subject
    GradleSemanticReleaseCommitMessageConventions conventions = new GradleSemanticReleaseCommitMessageConventions()

    def "finds references tickets one on each line"() {
        given:
        Commit commit = new Commit(fullMessage: '''\
            Closes #123
            Fixes #456
        '''.stripIndent())

        expect:
        conventions.closes(commit) == ['123', '456'] as SortedSet
    }

    def "finds references tickets all on one line"() {
        given:
        Commit commit = new Commit(fullMessage: '''\
            Closes #123, #456
        '''.stripIndent())

        expect:
        conventions.closes(commit) == ['123', '456'] as SortedSet
    }

    def "finds breaking change on same line"() {
        given:
        Commit commit = new Commit(fullMessage: '''\
            Subject

            BREAKING CHANGE: foo bar baz
        '''.stripIndent())

        expect:
        conventions.breaks(commit) == 'foo bar baz'
    }

    def "finds breaking change on next lines"() {
        given:
        Commit commit = new Commit(fullMessage: '''\
            Subject

            BREAKING CHANGE:

            foo bar baz
        '''.stripIndent())

        expect:
        conventions.breaks(commit) == 'foo bar baz'
    }

}
