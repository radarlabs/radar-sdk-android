package io.radar.mvnpublish

import io.github.gradlenexus.publishplugin.internal.NexusClient
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Reusable Maven Publishing container for Radar libraries
 */
class RadarMavenPublishPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply 'maven-publish'
        project.pluginManager.apply 'signing'
        RadarMavenPublishPluginExtension extension =
                project.extensions.create('mvnpublish', RadarMavenPublishPluginExtension, project)

        project.tasks.create('releaseSdkToMavenCentral') {
            group = 'publishing'
            description = 'Release the artifact to maven central and close the staging repo.'
            NexusClient client = new NexusClient(
                    new URI('https://s01.oss.sonatype.org/service/local/'),
                    System.getenv('NEXUS_USERNAME'),
                    System.getenv('NEXUS_PASSWORD'),
                    null,
                    null
            )
            doLast {
                String stagingInfoId = client.findStagingProfileId(extension.publicationGroup)
                client.releaseStagingRepository(stagingInfoId, extension.publicationDescription)
                logger.lifecycle("\tReleased the remote staging repository.")
                client.closeStagingRepository(stagingInfoId, extension.publicationDescription)
                logger.lifecycle("\tClosed the remote staging repository.")
            }
        }
    }
}