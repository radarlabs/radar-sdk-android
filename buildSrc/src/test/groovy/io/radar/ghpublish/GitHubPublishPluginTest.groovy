package io.radar.ghpublish

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

/**
 * Test {@link GitHubPublishPlugin}
 */
class GitHubPublishPluginTest {

    @Test
    void testApply() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'io.radar.ghpublish'
        assert project.ghpublish && project.ghpublish instanceof GitHubPublishPluginExtension
        assert project.tasks.findByName('createGitHubRelease')
    }
}
