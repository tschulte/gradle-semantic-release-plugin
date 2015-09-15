<a name="${version}"></a>
<%= if (version.endsWith('.0')) '#' else '##' %> <%= if (versionUrl) "[$version]($versionUrl)" else version %><%= if (title) " \"$title\"" else "" %><%= if (date) " ($date)" else "" %>
<%
def commitText(commit) {
    def closes = service.closes(commit).collect { "#$it" }
    "${service.subject(commit)} (${service.commitish(commit)}${closes ? (', closes ' + closes.join(', ')) : ''})"
}
def changeGroup(String title, group) {
    if (group) {
        println()
        println "### $title"
        println()
        group.each { component, commits ->
            if (!component) {
                commits.each { commit ->
                    println "* ${commitText(commit)}"
                }
            } else {
                print "* ${component ? "**$component:**" : ""}"
                if (component && commits.size() == 1)
                    println " ${commitText(commits[0])}"
                else {
                    println()
                    commits.each { commit ->
                        println "    * ${commitText(commit)}"
                    }
                }
            }
        }
    }
}
changeGroup("Bug Fixes", fix)
changeGroup("Features", feat)
changeGroup("Performance Improvements", perf)
changeGroup("Reverts", revert)

if (breaks) {
    println()
    println "### BREAKING CHANGES"
    println()
    breaks.each {
        println "* $it"
    }
}
%>