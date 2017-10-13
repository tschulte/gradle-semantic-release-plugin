package de.gliderpilot.gradle.semanticrelease

import com.jcabi.github.Coordinates
import com.jcabi.github.Release
import com.jcabi.github.ReleaseAssets
import com.jcabi.github.Repo
import com.jcabi.http.Request
import com.jcabi.http.request.FakeRequest

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class GhEnterpriseReleaseAssetsSpec extends Specification {
    @Shared
    @Subject
    GhEnterpriseReleaseAssets unit

    Request mockRequest
    Release mockRelease

    def setup() {
        def mockRepo = Mock(Repo.class)
        mockRepo.coordinates() >> Mock(Coordinates.class)
        mockRepo.coordinates().user() >> "test-user"
        mockRepo.coordinates().repo() >> "test-repo"

        mockRequest = new FakeRequest(
                HttpURLConnection.HTTP_CREATED,
                "fake request",
                Collections.<Map.Entry<String, String>>emptyList(),
                "{ \"id\": 1337 }".bytes
                )
        mockRelease = Mock()

        mockRelease.repo() >> mockRepo
        mockRelease.number() >> 1337
        mockRelease.assets() >> Mock(ReleaseAssets.class)

        unit = new GhEnterpriseReleaseAssets("https://enterprise.github", mockRelease, mockRequest)
    }

    def "should construct correct github upload api base url"() {
        when:
        unit = new GhEnterpriseReleaseAssets("https://enterprise.github", mockRelease, mockRequest)

        then:
        unit.githubUploadEndpoint.toString() == "https://enterprise.github/api/uploads"
    }

    def "should construct correct upload request with given data"() {
        given:
        def byte[] content = "This is the content"

        when:
        def uploadRequest = unit.getAssetUploadRequest(content, "test-type", "test-name")

        then:
        uploadRequest.toString().contains "test-user"
        uploadRequest.toString().contains "test-repo"
        uploadRequest.toString().contains "test-type"
        uploadRequest.uri().toString().contains "test-name"
        uploadRequest.body().get() == "This is the content"
    }

    def "should send an asset upload request and get the asset id"() {
        when:
        unit.upload("test".bytes, "test-type", "test-name")

        then:
        1 * mockRelease.assets().get(1337)
    }

    def "should pass method calls to underlying response class"() {
        when:
        unit.release()
        unit.iterate()
        unit.get(0)

        then:
        1 * mockRelease.assets().release()
        1 * mockRelease.assets().iterate()
        1 * mockRelease.assets().get(0)
    }
}
