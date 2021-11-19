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
        } else {
            assert !GitHubActions.get()
        }
    }

    @Test
    void testGetTag() {
        assert GitHubActions.getTag('refs/tags/3.2.2') == '3.2.2'
    }
}
