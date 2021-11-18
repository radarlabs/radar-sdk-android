package io.radar.mvnpublish

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

/**
 * Unit tests {@link RadarMavenPublishPlugin}
 */
class RadarMavenPublishPluginTest {

    @Test
    void testApply() {
        Project project = ProjectBuilder.builder().build()
        assert !project.pluginManager.findPlugin('maven-publish')
        assert !project.pluginManager.findPlugin('signing')
        project.pluginManager.apply 'io.radar.mvnpublish'
        assert project.pluginManager.findPlugin('maven-publish')
        assert project.pluginManager.findPlugin('signing')
        project.task([type: Jar], 'buildJar') {
            from project.buildDir
        }
        assert project.mvnpublish && project.mvnpublish instanceof RadarMavenPublishPluginExtension
        project.pluginManager.apply 'java'
        RadarMavenPublishPluginExtension extension = project.mvnpublish
        extension.publication {
            name = 'Radar Plugin Test'
            description = 'Test that the plugin works as expected'
            repositoryName = 'radar-sdk-test'
            group = 'io.radar'
            artifactId = 'test'
            version = "20.jay.st"
            artifacts = ["$project.buildDir/file.aar", project.buildJar]
            server = MavenServer.STAGING
        }
        project.evaluate()
        PublishingExtension publishingExtension = project.publishing
        assert project.tasks.findByName('publishSdkPublicationToMavenLocal')
        assert project.tasks.findByName('publishSdkPublicationToMavenRepository')
        assert publishingExtension.publications.size() == 1
        Publication publication = publishingExtension.publications[0]
        assert publication.name == 'sdk'
        assert publication instanceof MavenPublication
        MavenPublication mvn = publication
        assert mvn.groupId == 'io.radar'
        assert mvn.artifactId == 'test'
        assert mvn.version == '20.jay.st'

        assert publishingExtension.repositories.size() == 1
        assert publishingExtension.repositories[0] instanceof MavenArtifactRepository
        MavenArtifactRepository artifactory = publishingExtension.repositories[0]
        assert artifactory.url.toString() == MavenServer.STAGING.url

        assert project.signing && project.signing instanceof SigningExtension
        assert project.tasks.findByName('signSdkPublication')

    }
}
