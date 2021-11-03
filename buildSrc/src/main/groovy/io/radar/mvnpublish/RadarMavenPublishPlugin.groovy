package io.radar.mvnpublish

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Reusable Maven Publishing container for Radar libraries
 */
class RadarMavenPublishPlugin implements Plugin<Project> {

    private static final String RADAR_LABS = 'Radar Labs, Inc.'
    private static final String RADAR_URL = 'https://radar.io'
    private static final String USERNAME = 'nexusUsername'
    private static final String PASSWORD = 'nexusPassword'
    private static final String EMPTY_STRING = ''

    @Override
    void apply(Project project) {
        project.pluginManager.apply('maven-publish')
        project.pluginManager.apply('signing')

        project.extensions.create('mvnpublish', RadarMavenPublishPluginExtension, project)
    }
}