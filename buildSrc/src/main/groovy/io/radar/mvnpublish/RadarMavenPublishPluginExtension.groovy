package io.radar.mvnpublish

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.internal.credentials.DefaultPasswordCredentials

class RadarMavenPublishPluginExtension {

    private static final String RADAR_LABS = 'Radar Labs, Inc.'
    private static final String RADAR_URL = 'https://radar.io'
    private static final String USERNAME = 'nexusUsername'
    private static final String PASSWORD = 'nexusPassword'
    private static final String EMPTY_STRING = ''

    private final Project project

    RadarMavenPublishPluginExtension(Project project) {
        this.project = project
    }

    void publication(Closure closure) {
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        RadarPublication publication = new DefaultRadarPublication(project.objects)
        closure.setDelegate(publication)
        closure.run()
        String username = project.properties[USERNAME] ?: System.getenv('NEXUS_USERNAME') ?: EMPTY_STRING
        String password = project.properties[PASSWORD] ?: System.getenv('NEXUS_PASSWORD') ?: EMPTY_STRING
        DefaultPasswordCredentials credentials = new DefaultPasswordCredentials(username, password)
        DependencySet projectDependencies = project.configurations.implementation.allDependencies
        project.publishing {//TODO likely a scoping issue using closures instead of real objects.
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
                            artifactory.url publication.server.url
                            artifactory.credentials {
                                username = credentials.username
                                password = credentials.password
                            }
                        }

                    }

                }
            }
        }

        project.signing {
            sign project.publishing.publications.sdk
        }
    }
}
