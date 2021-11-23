package io.radar.mvnpublish

import groovy.transform.CompileDynamic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Reusable Maven Publishing container for Radar libraries
 */
@CompileDynamic
class RadarMavenPublishPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply 'maven-publish'
        project.pluginManager.apply 'signing'
        RadarMavenPublishPluginExtension extension =
                project.extensions.create('mvnpublish', RadarMavenPublishPluginExtension, project)

        Task prePublish = project.tasks.create('prePublish') {
            group = 'publishing'
            description = '''Runs before publishing an artifact, in order to ensure the nexus staging environment
meets expected preconditions.'''
            RadarNexusClient client = new RadarNexusClient(
                    System.getenv('NEXUS_USERNAME'), System.getenv('NEXUS_PASSWORD'))
            RadarRepositoryTransitioner transitioner = new RadarRepositoryTransitioner(client)
            doLast {
                String stagingInfoId = client.findStagingProfileId(extension.publicationGroup)
                List<String> repositoryIds = client.getRepositoryIdsFromProfile(stagingInfoId)
                if (!repositoryIds.empty) {
                    transitioner.effectivelyDrop(repositoryIds, 'Clean up stale staging repositories.')
                    project.logger.lifecycle('\tDropped the remote staging repositories.')
                }
            }
        }
        project.tasks.findByName('publish').dependsOn(prePublish)

        project.tasks.create('releaseSdkToMavenCentral') {
            group = 'publishing'
            description = 'Release the artifact to maven central and close the staging repo.'
            RadarNexusClient client = new RadarNexusClient(
                    System.getenv('NEXUS_USERNAME'), System.getenv('NEXUS_PASSWORD'))
            RadarRepositoryTransitioner transitioner = new RadarRepositoryTransitioner(client)
            doLast {
                String stagingInfoId = client.findStagingProfileId(extension.publicationGroup)
                String repositoryId = client.getRepositoryIdsFromProfile(stagingInfoId)[0]

                transitioner.effectivelyClose(repositoryId, extension.publicationDescription)
                project.logger.lifecycle('\tClosed the remote staging repository.')
                transitioner.effectivelyRelease(repositoryId, extension.publicationDescription)
                project.logger.lifecycle('\tReleased the remote staging repository.')
            }
        }
    }
}