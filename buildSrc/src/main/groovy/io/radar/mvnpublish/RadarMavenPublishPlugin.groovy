package io.radar.mvnpublish

import io.github.gradlenexus.publishplugin.internal.BasicActionRetrier
import io.github.gradlenexus.publishplugin.internal.StagingRepositoryTransitioner
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
            RadarNexusClient client = new RadarNexusClient(
                    System.getenv('NEXUS_USERNAME'), System.getenv('NEXUS_PASSWORD'))
            doLast {
                String stagingInfoId = client.findStagingProfileId(extension.publicationGroup)
                RadarNexusClient.ProfileRepository repository = client.getRepositoryIdFromProfile(stagingInfoId)

                StagingRepositoryTransitioner transitioner = new StagingRepositoryTransitioner(client,
                        new BasicActionRetrier(60, Duration.ofSeconds(10), repository.transitioning))
                transitioner.effectivelyClose(repository.repositoryId, extension.publicationDescription)
                logger.lifecycle("\tClosed the remote staging repository.")
                transitioner.effectivelyRelease(repository.repositoryId, extension.publicationDescription)
                logger.lifecycle("\tReleased the remote staging repository.")
            }
        }
    }
}