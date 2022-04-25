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
            tag = getTag ref
        }
        runNumber = System.getenv 'GITHUB_RUN_NUMBER'
    }

    static GitHubActions get() {
        if (Boolean.parseBoolean(System.getenv('GITHUB_ACTIONS'))) {
            return new GitHubActions()
        }
        null
    }

    /**
     * This assumes that the given ref uses the syntax 'refs/tags' and not 'refs/heads' or anything else.
     *
     * @param ref tag ref
     * @return the tag for the given tag ref
     */
    static String getTag(String ref) {
        ref[10..-1]
    }

}
