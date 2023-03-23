package io.radar.ci

/**
 * Helper class for use with GitHub Actions
 */
class GitHubActions {

    final String ref
    final String tag
    final boolean snapshot
    final String runNumber

    private GitHubActions() {
        ref = System.getenv 'GITHUB_REF'
        snapshot = Boolean.valueOf System.getenv('SNAPSHOT')
        if (snapshot) {
            tag = null
        } else {
            tag = System.getenv 'GITHUB_REF_NAME'
        }
        runNumber = System.getenv 'GITHUB_RUN_NUMBER'
    }

    static GitHubActions get() {
        if (Boolean.parseBoolean(System.getenv('GITHUB_ACTIONS'))) {
            return new GitHubActions()
        }
        null
    }

}
