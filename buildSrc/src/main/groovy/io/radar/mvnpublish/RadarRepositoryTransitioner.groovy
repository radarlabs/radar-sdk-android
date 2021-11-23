package io.radar.mvnpublish

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import io.github.gradlenexus.publishplugin.RepositoryTransitionException
import io.github.gradlenexus.publishplugin.internal.ActionRetrier
import io.github.gradlenexus.publishplugin.internal.BasicActionRetrier
import io.github.gradlenexus.publishplugin.internal.StagingRepository
import io.github.gradlenexus.publishplugin.internal.StagingRepositoryTransitioner
import kotlin.jvm.functions.Function0
import kotlin.jvm.functions.Function1

import java.time.Duration

@CompileStatic
class RadarRepositoryTransitioner {

    private final RadarNexusClient client
    private final ActionRetrier<StagingRepository> retrier
    private final StagingRepositoryTransitioner transitioner

    @PackageScope
    RadarRepositoryTransitioner(RadarNexusClient client) {
        this.client = client
        this.retrier = new BasicActionRetrier<StagingRepository>(60, Duration.ofSeconds(10),
                new Function1<StagingRepository, Boolean>() {

                    @Override
                    Boolean invoke(StagingRepository stagingRepository) {
                        stagingRepository.transitioning
                    }

                })
        this.transitioner = new StagingRepositoryTransitioner(client, retrier)
    }

    void effectivelyClose(String repoId, String description) {
        transitioner.effectivelyClose(repoId, description)
    }

    void effectivelyRelease(String repoId, String description) {
        transitioner.effectivelyRelease(repoId, description)
    }

    void effectivelyDrop(List<String> repoIds, String description) {
        effectivelyChangeState(repoIds.last(), StagingRepository.State.NOT_FOUND) {
            client.dropRepositories(repoIds, description)
        }
    }

    private void effectivelyChangeState(String repoId, StagingRepository.State desiredState, Closure transitionClientRequest) {
        transitionClientRequest.call(repoId)
        StagingRepository readStagingRepository = waitUntilTransitionIsDoneOrTimeoutAndReturnLastRepositoryState(repoId)
        assertRepositoryNotTransitioning(readStagingRepository)
        assertRepositoryInDesiredState(readStagingRepository, desiredState)
    }

    private StagingRepository waitUntilTransitionIsDoneOrTimeoutAndReturnLastRepositoryState(String repoId) {
        retrier.execute(new Function0<StagingRepository>() {

            @Override
            StagingRepository invoke() {
                client.getStagingRepositoryStateById(repoId)
            }

        })
    }

    private static void assertRepositoryNotTransitioning(StagingRepository repository) {
        if (repository.transitioning) {
            throw new RepositoryTransitionException(
                    "Staging repository is still transitioning after defined time. Consider its increment. $repository")
        }
    }

    private static void assertRepositoryInDesiredState(StagingRepository repository, StagingRepository.State desiredState) {
        if (repository.state != desiredState) {
            throw new RepositoryTransitionException("Staging repository is not in desired state $desiredState")
        }
    }
}