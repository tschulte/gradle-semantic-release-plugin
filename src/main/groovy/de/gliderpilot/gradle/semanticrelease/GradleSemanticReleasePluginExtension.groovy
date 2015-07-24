package de.gliderpilot.gradle.semanticrelease

import org.ajoberstar.gradle.git.release.semver.PartialSemVerStrategy
import org.ajoberstar.grgit.Grgit
import org.gradle.util.ConfigureUtil

/**
 * Created by tobias on 7/21/15.
 */
class GradleSemanticReleasePluginExtension {

    final Grgit grgit = Grgit.open()
    final PartialSemVerStrategy semanticStrategy = new GradleSemanticReleaseStrategy(grgit)
    final PartialSemVerStrategy onReleaseBranch = new GradleSemanticReleaseCheckReleaseBranchStrategy()
    final PartialSemVerStrategy appendBranchName = new GradleSemanticReleaseAppendBranchNameStrategy()

    def releaseBranches(Closure closure) {
        ConfigureUtil.configure(closure, onReleaseBranch)
    }

    def appendBranchNames(Closure closure) {
        ConfigureUtil.configure(closure, appendBranchName)
    }

}
