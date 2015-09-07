<a name="${version}"></a>
<%= if (version.endsWith('.0')) '#' else '##' %> <%= if (versionUrl) "[$version]($versionUrl)" else version %><%= if (title) " \"$title\"" else "" %><%= if (date) " ($date)" else "" %>
<%
def changeGroup(String title, group) {
    if (group) {
        println()
        println "### $title"
        println()
        group.each { component, commits ->
            if (!component) {
                commits.each { commit ->
                    println "* ${service.subject(commit)} (${service.commitish(commit)})"
                }
            } else {
                print "* ${component ? "**$component:**" : ""}"
                if (component && commits.size() == 1)
                    println " ${service.subject(commits[0])} (${service.commitish(commits[0])})"
                else {
                    println()
                    commits.each { commit ->
                        println "    * ${service.subject(commit)} (${service.commitish(commit)})"
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