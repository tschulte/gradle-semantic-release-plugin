package de.gliderpilot.gradle.semanticrelease

import org.ajoberstar.gradle.git.release.semver.PartialSemVerStrategy
import org.ajoberstar.gradle.git.release.semver.SemVerStrategy
import org.ajoberstar.gradle.git.release.semver.StrategyUtil
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 * Created by tobias on 7/21/15.
 */
class GradleSemanticReleasePluginExtension {

    final Grgit grgit
    final GradleSemanticReleaseCommitMessageConventions commitMessageConventions
    final PartialSemVerStrategy semanticStrategy
    final PartialSemVerStrategy onReleaseBranch
    final PartialSemVerStrategy appendBranchName

    @Inject
    GradleSemanticReleasePluginExtension(Project project) {
        grgit = Grgit.open()
        commitMessageConventions = new GradleSemanticReleaseCommitMessageConventions()
        semanticStrategy = new GradleSemanticReleaseStrategy(grgit, commitMessageConventions, project.release.tagStrategy)
        onReleaseBranch = new GradleSemanticReleaseCheckReleaseBranchStrategy()
        appendBranchName = new GradleSemanticReleaseAppendBranchNameStrategy()
    }

    def commitMessages(Closure closure) {
        ConfigureUtil.configure(closure, commitMessageConventions)
    }

    def releaseBranches(Closure closure) {
        ConfigureUtil.configure(closure, onReleaseBranch)
    }

    def appendBranchNames(Closure closure) {
        ConfigureUtil.configure(closure, appendBranchName)
    }

    SemVerStrategy toSemanticReleaseStrategy(SemVerStrategy strategy) {
        strategy.copyWith(
                normalStrategy: semanticStrategy,
                preReleaseStrategy: StrategyUtil.all(appendBranchName, strategy.preReleaseStrategy),
                buildMetadataStrategy: StrategyUtil.all(strategy.buildMetadataStrategy, onReleaseBranch)
        )
    }

}
