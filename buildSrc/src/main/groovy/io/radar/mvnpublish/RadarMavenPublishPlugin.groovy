package io.radar.mvnpublish

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
            RadarNexusClient client = new RadarNexusClient()
            doLast {
                String stagingInfoId = client.findStagingProfileId(extension.publicationGroup)
                String repositoryId = client.getRepositoryIdFromProfile(stagingInfoId)
                client.releaseStagingRepository(repositoryId, extension.publicationDescription)
                logger.lifecycle("\tReleased the remote staging repository.")
                client.closeStagingRepository(repositoryId, extension.publicationDescription)
                logger.lifecycle("\tClosed the remote staging repository.")
            }
        }
    }
}