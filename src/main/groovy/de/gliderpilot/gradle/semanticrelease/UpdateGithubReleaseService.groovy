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

import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import com.github.zafarkhaja.semver.Version
import com.jcabi.github.Coordinates
import com.jcabi.github.Release
import com.jcabi.github.ReleaseAsset
import com.jcabi.github.Repo

import groovy.transform.PackageScope

@PackageScope
class UpdateGithubReleaseService {

    private final Logger logger = Logging.getLogger(getClass())

    void updateGithubRelease(SemanticReleaseChangeLogService changeLog,
            GithubRepo githubRepo,
            ReleaseVersion version,
            String tagName) {
        Repo repo = githubRepo.github.repos().get(new Coordinates.Simple(githubRepo.mnemo))

        // check for the existance of the tag using the api -> #3
        long start = System.currentTimeMillis()
        while (!tagExists(repo, tagName) && System.currentTimeMillis() - start < 60000) {
        }

        Release release = repo.releases().create(tagName)
        def commits = changeLog.commits(Version.valueOf(version.previousVersion))

        Release uploadGate = new Release.Smart(release)
        uploadGate.body(changeLog.changeLog(commits, version).toString())

        def releaseAssets

        if (githubRepo.isGhEnterprise) { // handle assets for GitHub Enterprise
            releaseAssets = new GhEnterpriseReleaseAssets(githubRepo.getGhBaseUrl(), uploadGate, githubRepo.github.entry())
        } else { // handle assets for github.com
            releaseAssets = uploadGate.assets()
        }

        githubRepo.releaseAssets.each { asset ->
            ReleaseAsset releaseAsset = releaseAssets.upload(asset.file.bytes, asset.contentType, asset.name)
            if (asset.label) {
                new ReleaseAsset.Smart(releaseAsset).label(asset.label)
            }
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
