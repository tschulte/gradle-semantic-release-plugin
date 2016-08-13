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

import com.jcabi.github.Coordinates
import com.jcabi.github.Release
import com.jcabi.github.Repos
import com.jcabi.github.mock.MkGithub
import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.TagStrategy
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Timeout

class UpdateGithubReleaseServiceSpec extends Specification {

    Grgit grgit = Grgit.open()

    GithubRepo repo = new GithubRepo(grgit)

    TagStrategy tagStrategy = new TagStrategy()
    SemanticReleaseChangeLogService changeLogService = new SemanticReleaseChangeLogService(grgit, repo, tagStrategy)

    @Subject
    UpdateGithubReleaseService service = new UpdateGithubReleaseService()

    @Timeout(10)
    def "change log is uploaded to GitHub"() {
        given:
        File asset = new File('settings.gradle')
        repo.releaseAsset(asset)
        String mnemo = repo.mnemo
        String user = mnemo.substring(0, mnemo.indexOf("/"))
        String repoName = mnemo.substring(mnemo.indexOf("/") + 1)
        repo.github = new MkGithub(user)
        repo.github.repos().create(new Repos.RepoCreate(repoName, false))
        def coordinates = new Coordinates.Simple("$mnemo")
        repo.github.repos().get(coordinates).git().references().create("refs/tags/v1.1.0", "affe")
        changeLogService.changeLog = { List<Commit> commits, ReleaseVersion version ->
            "${'changelog'}"
        }

        when:
        service.updateGithubRelease(changeLogService,
                repo,
                new ReleaseVersion(version: "1.1.0", previousVersion: "1.0.0", createTag: true),
                "v1.1.0"
        )
        def releases = repo.github.repos().get(coordinates).releases()
        def release = releases.iterate().collect { new Release.Smart(it) }.find {
            it.tag() == 'v1.1.0'
        }

        then:
        release?.body() == 'changelog'
        release?.assets().iterate().any { it.json().getString("name") == asset.name }
    }

}
