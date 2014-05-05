package nl.tudelft.ewi.build.builds;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.docker.BuildReference;
import nl.tudelft.ewi.build.docker.DockerJob;
import nl.tudelft.ewi.build.docker.DockerManager;
import nl.tudelft.ewi.build.docker.Identifiable;
import nl.tudelft.ewi.build.docker.Logger;

@Slf4j
public class MockedDockerManager implements DockerManager {

	private final ListeningExecutorService mainExecutor;
	private final ExecutorService cleaningExecutor;
	private final Map<Identifiable, ListenableFuture<?>> futures;
	
	private final int jobDuration;

	public MockedDockerManager(int concurrentJobs, int jobDuration) {
		this.mainExecutor = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(concurrentJobs));
		this.cleaningExecutor = Executors.newScheduledThreadPool(concurrentJobs);
		this.futures = Maps.newConcurrentMap();
		this.jobDuration = jobDuration;
	}

	@Override
	public BuildReference run(final Logger logger, final DockerJob job) {
		final Identifiable containerId = new Identifiable();
		containerId.setId(UUID.randomUUID().toString());

		ListenableFuture<?> future = mainExecutor.submit(new Runnable() {
			@Override
			public void run() {
				log.info("Started: {}", containerId);
				logger.onStart();
				
				long started = System.currentTimeMillis();
				while (started + jobDuration < System.currentTimeMillis()) {
					logger.onNextLine("Writing to log...");
					try {
						Thread.sleep(500);
					}
					catch (InterruptedException e) {
						log.error(e.getMessage(), e);
					}
				}
				
				logger.onClose(0);
				log.info("Terminated: {}", containerId);
			}
		});
		
		log.info("Submitted job: {}", job);
		futures.put(containerId, future);
		Runnable cleaner = new Runnable() {
			@Override
			public void run() {
				futures.remove(containerId);
				log.info("Removed job: {}", job);
			}
		};
		
		future.addListener(cleaner, cleaningExecutor);
		
		return new BuildReference(job, containerId, future);
	}

	@Override
	public boolean terminate(Identifiable containerId) {
		ListenableFuture<?> future = futures.get(containerId);
		if (future != null) {
			future.cancel(true);
			return true;
		}
		return false;
	}

	@Override
	public int getActiveJobs() {
		return futures.size();
	}

}
