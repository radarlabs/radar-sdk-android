package io.radar.mvnpublish

import groovy.transform.PackageScope
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.publish.maven.MavenPublication

/**
 * Configures publication tasks for Radar modules
 */
class RadarMavenPublishPluginExtension {

    private static final String RADAR_LABS = 'Radar Labs, Inc.'
    private static final String RADAR_URL = 'https://radar.io'

    private final Project project

    @PackageScope
    String publicationGroup
    @PackageScope
    String publicationDescription

    RadarMavenPublishPluginExtension(Project project) {
        this.project = project
    }

    void publication(Closure closure) {
        if (publicationGroup != null) {
            // This plugin is not designed to support multiple publications within a single module.
            project.logger.warn 'Publication already configured'
            return
        }
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        RadarPublication publication = new RadarPublication(project.objects)
        closure.setDelegate(publication)
        closure.run()
        publicationGroup = publication.group.get()
        publicationDescription = publication.description.get()
        DependencySet projectDependencies = project.configurations.implementation.allDependencies
        boolean isSnapshot = publication.version.get().endsWith('-SNAPSHOT')
        project.publishing {
            publications {
                sdk(MavenPublication) {
                    groupId publication.group.get()
                    artifactId publication.artifactId.get()
                    version publication.version.get()
                    artifacts = publication.artifacts

                    pom {
                        name = publication.name.get()
                        description = publication.description.get()
                        url = RADAR_URL

                        licenses {
                            license {
                                name = 'Apache License, Version 2.0'
                                url = 'http://www.apache.org/licenses/LICENSE-2.0.txt'
                            }
                        }

                        organization {
                            name = RADAR_LABS
                            url = RADAR_URL
                        }

                        developers {
                            developer {
                                id = 'radarlabs'
                                name = RADAR_LABS
                            }
                        }

                        scm {
                            connection = "scm:git:git://github.com:radarlabs/${publication.repositoryName.get()}.git"
                            developerConnection = "scm:git:ssh://github.com:radarlabs/${publication.repositoryName.get()}.git"
                            url = "https://github.com/radarlabs/${publication.repositoryName.get()}"
                        }

                        withXml {
                            Node dependenciesNode = asNode().appendNode('dependencies')
                            projectDependencies.each { dependency ->
                                if (dependency.group && dependency.name && dependency.version) {
                                    Node dependencyNode = dependenciesNode.appendNode('dependency')
                                    dependencyNode.appendNode('groupId', dependency.group)
                                    dependencyNode.appendNode('artifactId', dependency.name)
                                    dependencyNode.appendNode('version', dependency.version)
                                }
                            }
                        }
                    }
                }
            }

            repositories new Action<RepositoryHandler>() {

                @Override
                void execute(RepositoryHandler repositories) {
                    repositories.maven new Action<MavenArtifactRepository>() {

                        @Override
                        void execute(MavenArtifactRepository artifactory) {
                            artifactory.url = (isSnapshot ? MavenServer.SNAPSHOT : MavenServer.STAGING).url
                            artifactory.credentials new Action<PasswordCredentials>() {

                                @Override
                                void execute(PasswordCredentials passwordCredentials) {
                                    passwordCredentials.username = System.getenv 'NEXUS_USERNAME'
                                    passwordCredentials.password = System.getenv 'NEXUS_TOKEN'
                                }

                            }

                        }

                    }

                }
            }
        }

        project.signing {
            String signingKey = project.findProperty('SIGNINGKEY')
            String signingPassword = project.findProperty('SIGNINGPASSWORD')
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign project.publishing.publications.sdk
        }

        if (publication.server == MavenServer.RELEASE) {
            if (isSnapshot) {
                throw new IllegalArgumentException('Snapshot builds cannot be promoted to release.')
            } else {
                project.tasks.findByName('publish').finalizedBy(project.tasks.findByName('releaseSdkToMavenCentral'))
            }
        }
    }
}
