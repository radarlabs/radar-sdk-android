package io.radar.ci

import io.radar.mvnpublish.MavenServer
import org.json.JSONObject

import java.util.concurrent.TimeoutException

/**
 * Utility for sending a repository dispatch for SNAPSHOT releases.
 */
class SnapshotDispatcher {

    /**
     * Blocking call to send toolkit dispatch with information about the given SDK version
     *
     * @param username GitHub Username
     * @param token GitHub personal access token
     * @param version SNAPSHOT version
     * @param event snapshot event type
     * @return the API response
     */
    static GitHubClient.ApiResponse dispatchSnapshotEvent(String username, String token, String version, String event = 'snapshot') {
        JSONObject json = new JSONObject()
        json.put('version', version)
        json.put('repo', MavenServer.SNAPSHOT.url)
        List<GitHubClient.ApiResponse> response = []
        GitHubClient.repositoryDispatch(event, json, username, token) { response.add it }
        int counter = 0
        while (response.empty) {
            if (counter == 10) {
                throw new TimeoutException('No response received from dispatch event')
            }
            Thread.sleep(1000)
            counter++
        }
        response[0]
    }

}
