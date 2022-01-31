package io.radar.mvnpublish

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskDependency
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

/**
 * Unit tests {@link RadarMavenPublishPlugin}
 */
class RadarMavenPublishPluginTest {

    @ParameterizedTest
    @MethodSource("provideParameters")
    void testApply(MavenServer mavenServer, boolean isSnapshot) {
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
        Closure pluginConfig = {
            name = 'Radar Plugin Test'
            description = 'Test that the plugin works as expected'
            repositoryName = 'radar-sdk-test'
            group = 'io.radar'
            artifactId = 'test'
            version = "20.jay.st${isSnapshot ? '-SNAPSHOT' : ''}"
            artifacts = ["$project.buildDir/file.aar", project.buildJar]
            server = mavenServer
        }
        if (mavenServer == MavenServer.RELEASE && isSnapshot) {
            Assertions.assertThrows(IllegalArgumentException) {
                extension.publication pluginConfig
            }
            return
        }
        extension.publication pluginConfig
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
        if (isSnapshot) {
            assert mvn.version == '20.jay.st-SNAPSHOT'
        } else {
            assert mvn.version == '20.jay.st'
        }

        assert publishingExtension.repositories.size() == 1
        assert publishingExtension.repositories[0] instanceof MavenArtifactRepository
        MavenArtifactRepository artifactory = publishingExtension.repositories[0]
        if (isSnapshot) {
            assert artifactory.url.toString() == MavenServer.SNAPSHOT.url
        } else {
            assert artifactory.url.toString() == MavenServer.STAGING.url
        }

        assert project.signing && project.signing instanceof SigningExtension
        assert project.tasks.findByName('signSdkPublication')

        Task prePublishTask = project.tasks.findByName('prePublish')
        assert prePublishTask

        Task releaseTask = project.tasks.findByName('releaseSdkToMavenCentral')
        assert releaseTask

        Task publishTask = project.tasks.findByName('publish')
        assert publishTask.dependsOn.contains(prePublishTask)

        TaskDependency publishDependencies = publishTask.finalizedBy
        if (mavenServer == MavenServer.RELEASE) {
            assert publishDependencies.getDependencies(null).contains(releaseTask)
        } else {
            assert !publishDependencies.getDependencies(null).contains(releaseTask)
        }
    }

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
                Arguments.of(MavenServer.SNAPSHOT, false),
                Arguments.of(MavenServer.SNAPSHOT, true),
                Arguments.of(MavenServer.RELEASE, false),
                Arguments.of(MavenServer.RELEASE, true),
        )
    }
}
