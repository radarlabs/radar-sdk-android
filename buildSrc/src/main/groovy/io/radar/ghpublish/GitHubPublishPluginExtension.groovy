package io.radar.ghpublish

import org.gradle.api.provider.Property

abstract class GitHubPublishPluginExtension {

    abstract Property<String> getTagName()

    boolean draft = false

    boolean prerelease = false

    boolean generateReleaseNotes = false
}
