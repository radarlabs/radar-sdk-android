package io.radar.mvnpublish

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPomScm

interface RadarPublication {

    Property<String> getName()

    Property<String> getDescription()

    Property<String> getRepositoryName()

    Property<String> getGroup()

    Property<String> getArtifactId()

    Property<String> getVersion()

    Iterable getArtifacts()

    MavenServer getServer()
}
