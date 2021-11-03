package io.radar.mvnpublish

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Reusable Maven Publishing container for Radar libraries
 */
class RadarMavenPublishPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply('maven-publish')
        project.pluginManager.apply('signing')
        project.extensions.create('mvnpublish', RadarMavenPublishPluginExtension, project)
    }
}