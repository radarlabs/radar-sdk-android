package io.radar.mvnpublish

import groovy.transform.CompileDynamic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

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
                    System.getenv('NEXUS_USERNAME'), System.getenv('NEXUS_TOKEN'))
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
                    System.getenv('NEXUS_USERNAME'), System.getenv('NEXUS_TOKEN'))
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

        project.tasks.create('triggerManualUpload') {
            group = 'publishing'
            description = 'Trigger manual upload to Maven Central after artifacts have been published.'
            doLast {
                String namespace = System.getenv('MAVEN_CENTRAL_NAMESPACE')
                if (!namespace) {
                    throw new IllegalStateException('MAVEN_CENTRAL_NAMESPACE environment variable is required for manual upload trigger')
                }

                String username = System.getenv('NEXUS_USERNAME')
                String password = System.getenv('NEXUS_TOKEN')
                if (!username || !password) {
                    throw new IllegalStateException('NEXUS_USERNAME and NEXUS_TOKEN environment variables are required for manual upload trigger')
                }

                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                String url = "https://central.sonatype.com/api/v1/manual/upload/defaultRepository/${namespace}"
                
                Request request = new Request.Builder()
                    .url(url)
                    .post(okhttp3.RequestBody.create("", null))
                    .addHeader("Authorization", "Basic " + java.util.Base64.encoder.encodeToString("${username}:${password}".getBytes()))
                    .addHeader("Content-Type", "application/json")
                    .build()

                try {
                    Response response = client.newCall(request).execute()
                    if (response.isSuccessful()) {
                        project.logger.lifecycle('\tSuccessfully triggered manual upload to Maven Central')
                    } else {
                        String errorBody = response.body()?.string() ?: "No error body"
                        project.logger.error("Failed to trigger manual upload. Status: ${response.code()}, Body: ${errorBody}")
                        throw new RuntimeException("Manual upload trigger failed with status ${response.code()}")
                    }
                } catch (Exception e) {
                    project.logger.error("Exception during manual upload trigger: ${e.message}")
                    throw e
                }
            }
        }

        // Make the manual upload trigger run after the release task
        project.tasks.findByName('releaseSdkToMavenCentral').finalizedBy(project.tasks.findByName('triggerManualUpload'))
    }
}