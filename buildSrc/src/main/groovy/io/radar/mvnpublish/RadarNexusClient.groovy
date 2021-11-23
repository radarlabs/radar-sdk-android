package io.radar.mvnpublish

import io.github.gradlenexus.publishplugin.internal.NexusClient
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers

import static io.github.gradlenexus.publishplugin.internal.NexusClient.*

/**
 * Extends the functionality of {@link NexusClient}
 */
class RadarNexusClient extends NexusClient {

    private static final URI BASE_URL = new URI('https://s01.oss.sonatype.org/service/local/')
    private static final String USERNAME = System.getenv 'NEXUS_USERNAME' ?: ''
    private static final String PASSWORD = System.getenv 'NEXUS_PASSWORD' ?: ''
    private final ApiExtensions api

    RadarNexusClient() {
        super(BASE_URL, USERNAME, PASSWORD, null, null)

        String credentials = Credentials.basic(USERNAME, PASSWORD)
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder()
                            .header('Authorization', credentials)
                            .build())
                }
                .addInterceptor { chain ->
                    String version = NexusClient.package.implementationVersion ?: 'dev'
                    chain.proceed(chain.request().newBuilder()
                            .header('User-Agent', "gradle-nexus-publish-plugin/$version")
                            .build())
                }
                .build()
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL.toString())
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        api = retrofit.create(ApiExtensions)
    }

    /**
     * Get the repository ID from the given staging profile ID
     *
     * @param stagingProfileId ID of the staging artifact profile
     * @return the repository ID for the given profile ID
     */
    String getRepositoryIdFromProfile(String stagingProfileId) {
        Response<ProfileRepository> response = api.profileRepositories.execute()
        if (response.successful) {
            Dto<List<ProfileRepository>> repositoriesResponse = response.body()
            for (ProfileRepository repository : repositoriesResponse.data) {
                if (repository.profileId == stagingProfileId) {
                    return repository.repositoryId
                }
            }
            throw new IllegalStateException('No repository for given profile ID')
        } else {
            String message = "Failed to load profile repositories, server at $BASE_URL responded with status code ${response.code()}"
            ResponseBody errorBody = response.errorBody()
            if (errorBody) {
                try {
                    message = "$message, $errorBody"
                } catch (IOException exception) {
                    message = "$message, body: <error while reading body of error, message: $exception.message"
                }
            }
            throw new RuntimeException(message)
        }
    }

    private interface ApiExtensions {
        @Headers('Accept: application/json')
        @GET('staging/profile_repositories')
        Call<Dto<List<ProfileRepository>>> getProfileRepositories()
    }

    class ProfileRepository {
        String profileId
        String repositoryId
    }
}