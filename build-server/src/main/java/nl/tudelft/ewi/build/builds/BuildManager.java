package nl.tudelft.ewi.build.builds;

import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.docker.DockerManager;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;

@Slf4j
@Singleton
public class BuildManager {
	
	private final ScheduledThreadPoolExecutor executorService;
	private final DockerManager dockerManager;
	private final Config config;
	
	@Inject
	public BuildManager(DockerManager dockerManager, Config config) {
		this.executorService = new ScheduledThreadPoolExecutor(config.getMaximumConcurrentJobs());
		this.dockerManager = dockerManager;
		this.config = config;
	}
	
	public UUID schedule(BuildRequest request) {
		log.info("Submitted job: " + request);
		if (tooBusy()) {
			log.info("Server is too busy!");
			return null;
		}
		
		UUID identifier = UUID.randomUUID();
		executorService.submit(new BuildRunner(dockerManager, config, request, identifier));
		return identifier;
	}

	private boolean tooBusy() {
		return executorService.getActiveCount() >= config.getMaximumConcurrentJobs();
	}

}
