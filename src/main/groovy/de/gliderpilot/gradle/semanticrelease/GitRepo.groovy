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

import groovy.transform.PackageScope
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.AbstractArchiveTask

abstract class GitRepo {

    private Map<File, Asset> releaseAssets = [:].withDefault { file -> new Asset(file) }

    @PackageScope
    Collection<Asset> getReleaseAssets() {
        return releaseAssets.values()
    }


    Asset releaseAsset(Map<String, String> params = [:], AbstractArchiveTask task) {
        params.builtBy = task
        releaseAsset(params, task.outputs.files.singleFile)
    }

    Asset releaseAsset(Map<String, String> params = [:], File file) {
        Asset asset = releaseAssets[file]
        params.each { key, value ->
            asset."$key" = value
        }
        asset
    }

    abstract String diffUrl(String previousTag, String currentTag)

    abstract String commitUrl(String abbreviatedId)
}

class Asset {
    final File file
    Task builtBy
    String name
    String label
    String contentType

    Asset(File file) {
        this.file = file
        name = file.name
        contentType = URLConnection.guessContentTypeFromName(name) ?: "application/octet-stream"
    }

    Asset name(String name) {
        this.name = name
        this
    }

    Asset label(String label) {
        this.label = label
        this
    }

    Asset contentType(String contentType) {
        this.contentType = contentType
        this
    }
}
