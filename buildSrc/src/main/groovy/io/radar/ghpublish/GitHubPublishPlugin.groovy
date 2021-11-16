package io.radar.ghpublish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.json.JSONObject

import javax.net.ssl.HttpsURLConnection

/**
 * Creates a configurable task to publish a GitHub release
 *
 * @see : <a href="https://docs.github.com/en/rest/reference/repos#create-a-release">Create a Release</a>
 */
class GitHubPublishPlugin implements Plugin<Project> {

    private static final String EXTENSION = 'ghpublish'

    @Override
    void apply(Project project) {
        GitHubPublishPluginExtension extension = project.extensions.create(EXTENSION, GitHubPublishPluginExtension)
        project.tasks.register('createGitHubRelease') {
            group 'publishing'
            description 'Publishes a release on GitHub'
            doLast {
                String url = extension.releasesUrl.get() ?:
                        "https://api.github.com/repos/$CIRCLE_PROJECT_USERNAME/$CIRCLE_PROJECT_REPONAME/releases"
                URL address = new URL(url)
                HttpsURLConnection connection = address.openConnection()
                connection.setRequestProperty 'accept', 'application/vnd.github.v3+json'
                connection.setRequestProperty 'Authentication', "token $GITHUB_TOKEN"
                JSONObject json = new JSONObject()
                json.with {
                    put 'tag_name', extension.tagName.get()
                    put 'draft', extension.draft
                    put 'prerelease', extension.prerelease
                    put 'generate_release_notes', extension.generateReleaseNotes
                }
                connection.requestMethod = 'POST'
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.outputStream)
                outputStreamWriter.write(json.toString())
                outputStreamWriter.close()

                if (connection.responseCode != 201) {
                    throw new IOException("Release not created. Error $connection.responseCode: $connection.responseMessage")
                }
            }
        }
    }

}