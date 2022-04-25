package io.radar.ci

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

/**
 * Tests {@link SnapshotDispatcher}
 */
class SnapshotDispatcherTest {

    @Test
    void testRepositoryDispatch() {
        String username = System.getProperty('PAT_USERNAME')
        String password = System.getProperty('PAT_TOKEN')
        Assumptions.assumeFalse(username == null || username.empty)
        Assumptions.assumeFalse(password == null || password.empty)
        GitHubClient.ApiResponse response = SnapshotDispatcher.dispatchSnapshotEvent(
                username, password, '3.3.0-SNAPSHOT', 'snapshot-test'
        )
        assert response.success : "Could not send repository dispatch! $response.status: ${response.message ?: ''}\n ${response.data ?: ''}"
    }

}
