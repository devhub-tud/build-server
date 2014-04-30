package nl.tudelft.ewi.build.builds;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.docker.DockerManager;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Slf4j
@Singleton
public class BuildManager {

	private final Map<UUID, ListenableFuture<?>> futures;
	private final ListeningExecutorService listeningService;
	private final ScheduledExecutorService schedulerService;
	private final DockerManager dockerManager;
	private final Config config;

	private final int concurrentJobs;
	private volatile Integer runningJobs = 0;

	@Inject
	public BuildManager(DockerManager dockerManager, Config config) {
		this.concurrentJobs = config.getMaximumConcurrentJobs();
		this.futures = Maps.newHashMap();
		this.schedulerService = Executors.newScheduledThreadPool(concurrentJobs);
		this.listeningService = MoreExecutors.listeningDecorator(schedulerService);
		this.dockerManager = dockerManager;
		this.config = config;
	}

	public UUID schedule(BuildRequest request) {
		log.info("Submitted job: " + request);
		synchronized (runningJobs) {
			if (runningJobs + 1 > concurrentJobs) {
				log.info("Server is too busy!");
				return null;
			}
			runningJobs = runningJobs + 1;
		}

		UUID identifier = UUID.randomUUID();
		BuildRunner runner = new BuildRunner(dockerManager, config, request, identifier);
		ListenableFuture<?> future = listeningService.submit(runner);
		future.addListener(createListener(), listeningService);
		futures.put(identifier, future);

		if (request.getTimeout() > 0) {
			schedulerService.schedule(createTerminator(identifier), request.getTimeout(), TimeUnit.MINUTES);
		}

		return identifier;
	}

	private Runnable createTerminator(final UUID identifier) {
		return new Runnable() {
			@Override
			public void run() {
				killBuild(identifier);
			}
		};
	}

	private Runnable createListener() {
		return new Runnable() {
			@Override
			public void run() {
				synchronized (runningJobs) {
					runningJobs = runningJobs - 1;
				}
			}
		};
	}

	public boolean killBuild(UUID buildId) {
		ListenableFuture<?> future = futures.get(buildId);
		if (future != null) {
			return future.cancel(true);
		}
		return false;
	}

}
