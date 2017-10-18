/*
 * Copyright 2017 the original author or authors.
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

import javax.ws.rs.core.HttpHeaders

import com.jcabi.github.Coordinates
import com.jcabi.github.Release
import com.jcabi.github.ReleaseAsset
import com.jcabi.github.ReleaseAssets
import com.jcabi.http.Request
import com.jcabi.http.response.JsonResponse
import com.jcabi.http.response.RestResponse

/** Adapter class for GitHub Enterprise API endpoints
 * 
 * 	RtReleaseAssets class is declared final. Therefore this class can not be extended and
 *  makes a wrapper/adapter class (like this) necessary.
 *  
 *  Contents of the methods of this class are modified versions of the RtReleaseAssets class
 *  (with support for GitHub Enterprise servers)
 */
public class GhEnterpriseReleaseAssets implements ReleaseAssets {

    private final Release owner
    private final Request entry

    final URI githubUploadEndpoint

    public GhEnterpriseReleaseAssets(final String githubBaseUrl, Release owner, Request entry) {
        this.githubUploadEndpoint = URI.create("${githubBaseUrl}/api/uploads") // base path for asset uploads
        this.entry = entry
        this.owner = owner
    }

    @Override
    public ReleaseAsset upload(final byte[] content, final String type, final String name) throws IOException {
        return this.get(
                getAssetUploadRequest(content, type, name)
                .fetch().as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_CREATED)
                .as(JsonResponse.class)
                .json().readObject().getInt("id")
            )
    }

    Request getAssetUploadRequest(final byte[] content, final String type, final String name) {
        return this.entry.uri()
                .set(this.githubUploadEndpoint)
                .path("/repos")
                .path(this.owner.repo().coordinates().user())
                .path(this.owner.repo().coordinates().repo())
                .path("/releases")
                .path(String.valueOf(this.owner.number()))
                .path("/assets")
                .queryParam("name", name)
                .back()
                .method(Request.POST)
                .reset(HttpHeaders.CONTENT_TYPE)
                .header(HttpHeaders.CONTENT_TYPE, type)
                .body().set(content).back()
    }

    @Override
    public Release release() {
        return owner.assets().release()
    }

    @Override
    public Iterable<ReleaseAsset> iterate() {
        return owner.assets().iterate()
    }

    @Override
    public ReleaseAsset get(final int number) {
        return owner.assets().get(number)
    }
}
