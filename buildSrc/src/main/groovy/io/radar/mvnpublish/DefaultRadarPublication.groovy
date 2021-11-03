package io.radar.mvnpublish

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

class DefaultRadarPublication implements RadarPublication {

    private String name

    private String description

    private String repositoryName

    private String group

    private String artifactId

    private String version

    Iterable artifacts = []

    MavenServer server = MavenServer.STAGING

    private final ObjectFactory objects

    DefaultRadarPublication(ObjectFactory objects) {
        this.objects = objects
    }

    private Property<String> getStringProperty(String value) {
        if (value) {
            Property<String> property = objects.property(String)
            property.set(value)
            property
        } else {
            throw new IllegalStateException('Missing value')
        }
    }

    void setName(String name) {
        this.name = name
    }

    @Override
    Property<String> getName() {
        getStringProperty(name)
    }

    void setDescription(String description) {
        this.description = description
    }

    @Override
    Property<String> getDescription() {
        getStringProperty(description)
    }

    void setGroup(String group) {
        this.group = group
    }

    @Override
    Property<String> getGroup() {
        getStringProperty(group)
    }

    void setArtifactId(String artifactId) {
        this.artifactId = artifactId
    }

    @Override
    Property<String> getArtifactId() {
        getStringProperty(artifactId)
    }

    void setVersion(String version) {
        this.version = version
    }

    @Override
    Property<String> getVersion() {
        getStringProperty(version)
    }

    void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName
    }

    @Override
    Property<String> getRepositoryName() {
        getStringProperty(repositoryName)
    }
}
