package nl.tudelft.ewi.build.builds;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.docker.DockerManager;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;

@Slf4j
@Singleton
public class BuildManager {

	private final Map<UUID, ListenableFuture<?>> futures;
	private final Map<UUID, BuildRunner> runners;

	private final ListeningExecutorService listeningService;
	private final ScheduledExecutorService schedulerService;
	private final DockerManager dockerManager;
	private final Config config;

	@Inject
	public BuildManager(DockerManager dockerManager, Config config) {
		this.futures = Maps.newConcurrentMap();
		this.runners = Maps.newConcurrentMap();

		this.schedulerService = Executors.newScheduledThreadPool(config.getMaximumConcurrentJobs());
		this.listeningService = MoreExecutors.listeningDecorator(schedulerService);
		this.dockerManager = dockerManager;
		this.config = config;
	}

	public UUID schedule(BuildRequest request) {
		log.info("Submitted job: " + request);
		if (futures.size() >= config.getMaximumConcurrentJobs()) {
			log.info("Server is too busy!");
			return null;
		}

		UUID buildId = UUID.randomUUID();
		BuildRunner runner = new BuildRunner(dockerManager, config, request, buildId);
		ListenableFuture<?> future = listeningService.submit(runner);
		futures.put(buildId, future);
		runners.put(buildId, runner);

		Integer timeout = request.getTimeout();
		if (timeout != null && timeout > 0) {
			log.debug("Build will automatically terminate in: {} seconds...", timeout);
			Runnable terminator = createTerminator(buildId);
			final Future<?> terminatorFuture = schedulerService.schedule(terminator, timeout, TimeUnit.SECONDS);
			future.addListener(cancelTerminator(buildId, terminatorFuture), schedulerService);
		}
		
		future.addListener(cleaner(buildId), schedulerService);

		return buildId;
	}

	private Runnable cleaner(final UUID buildId) {
		return new Runnable() {
			@Override
			public void run() {
				futures.remove(buildId);
				runners.remove(buildId);
			}
		};
	}

	private Runnable cancelTerminator(final UUID buildId, final Future<?> terminatorFuture) {
		return new Runnable() {
			@Override
			public void run() {
				Future<?> future = futures.remove(buildId);
				if (future != null && !future.isDone() && !future.isCancelled()) {
					log.debug("Cancelling terminator for build: {}", buildId);
					terminatorFuture.cancel(true);
				}
			}
		};
	}

	private Runnable createTerminator(final UUID buildId) {
		return new Runnable() {
			@Override
			public void run() {
				log.debug("Forcefully terminating build: {}", buildId);
				killBuild(buildId);
			}
		};
	}

	@SneakyThrows
	public boolean killBuild(UUID buildId) {
		BuildRunner runner = runners.remove(buildId);
		ListenableFuture<?> future = futures.remove(buildId);

		if (future != null) {
			future.cancel(true);
			if (runner != null) {
				runner.terminate();
			}
			return true;
		}
		return false;
	}

}
