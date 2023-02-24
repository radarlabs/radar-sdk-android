package io.radar.mvnpublish

import groovy.transform.CompileStatic
import io.github.gradlenexus.publishplugin.internal.NexusClient
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.jetbrains.annotations.NotNull
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

import static io.github.gradlenexus.publishplugin.internal.NexusClient.*

/**
 * Extends the functionality of {@link NexusClient}
 */
@CompileStatic
class RadarNexusClient extends NexusClient {

    private static final URI BASE_URL = new URI('https://s01.oss.sonatype.org/service/local/')
    private final ApiExtensions api

    /**
     * Create a new Radar Nexus Client
     *
     * @param username nexus username
     * @param password nexus password
     */
    RadarNexusClient(String username, String password) {
        super(BASE_URL, username, password, null, null)
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .writeTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .connectTimeout(5, TimeUnit.MINUTES)
                .addInterceptor { chain ->
                    String version = NexusClient.package.implementationVersion ?: 'dev'
                    chain.proceed(chain.request().newBuilder()
                            .header('User-Agent', "gradle-nexus-publish-plugin/$version")
                            .build())
                }
        if (username && password) {
            String credentials = Credentials.basic(username, password)
            httpClient.addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder()
                        .header('Authorization', credentials)
                        .build())
            }
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL.toString())
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        api = retrofit.create(ApiExtensions)
    }

    /**
     * Get the repository IDs from the given staging profile ID
     *
     * @param stagingProfileId ID of the staging artifact profile
     * @return the repository IDs for the given profile ID
     */
    List<String> getRepositoryIdsFromProfile(String stagingProfileId) {
        Response<Dto<List<ProfileRepository>>> response = api.profileRepositories.execute()
        if (response.successful) {
            List<String> repositoryIds = []
            Dto<List<ProfileRepository>> repositoriesResponse = response.body()
            for (ProfileRepository repository : repositoriesResponse.data.sort()) {
                if (repository.profileId == stagingProfileId) {
                    repositoryIds += repository.repositoryId
                }
            }
            repositoryIds
        } else {
            throw apiException("load profile repositories", response.code(), response.errorBody())
        }
    }

    /**
     * Drop all the give repositories
     *
     * @param repositoryIds IDs of the repositories to drop
     * @param description reason why the repos are being dropped
     */
    void dropRepositories(List<String> repositoryIds, String description) {
        Dto<StagingRepositoryToTransit> body = new Dto<StagingRepositoryToTransit>(
                new StagingRepositoryToTransit(repositoryIds, description, false))
        Response<Void> response = api.dropRepositories(body).execute()
        if (!response.successful) {
            throw apiException("drop the profile", response.code(), response.errorBody())
        }
    }

    /**
     * Formats an exception which can be thrown when an API response comes back in an error state
     *
     * @param action the action that was being performed
     * @param responseCode error code
     * @param errorBody response body containing the error details
     * @return an exception containing the given details
     */
    private static Exception apiException(String action, int responseCode, ResponseBody errorBody) {
        String message = "Failed to $action, server at $BASE_URL responded with status code $responseCode"
        if (errorBody) {
            try {
                message = "$message, $errorBody"
            } catch (IOException exception) {
                message = "$message, body: <error while reading body of error, message: $exception.message"
            }
        }
        return new RuntimeException(message)
    }

    private interface ApiExtensions {
        @Headers('Accept: application/json')
        @GET('staging/profile_repositories')
        Call<Dto<List<ProfileRepository>>> getProfileRepositories()

        @Headers('Accept: application/json')
        @POST('staging/bulk/drop')
        Call<Void> dropRepositories(@Body Dto<StagingRepositoryToTransit> stagingRepoToClose)
    }

    class ProfileRepository implements Comparable<ProfileRepository> {
        String profileId
        String repositoryId
        long createdTimestamp

        @Override
        int compareTo(@NotNull ProfileRepository profileRepository) {
            createdTimestamp <=> profileRepository.createdTimestamp
        }
    }

}