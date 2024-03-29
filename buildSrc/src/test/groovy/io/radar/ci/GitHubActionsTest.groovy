package io.radar.ci

import org.junit.jupiter.api.Test

/**
 * Tests {@link GitHubActions}
 */
class GitHubActionsTest {

    @Test
    void testGet() {
        if (Boolean.parseBoolean(System.getenv('GITHUB_ACTIONS'))) {
            assert GitHubActions.get()
            String snapshot = System.getenv 'SNAPSHOT'
            // Ensures that this variable is defined in GitHub Action workflows
            boolean isSnapshotEnvBoolean = 'true'.equalsIgnoreCase(snapshot) || 'false'.equalsIgnoreCase(snapshot)
            assert isSnapshotEnvBoolean : "SNAPSHOT: $snapshot"
        } else {
            assert !GitHubActions.get()
        }
    }
}
