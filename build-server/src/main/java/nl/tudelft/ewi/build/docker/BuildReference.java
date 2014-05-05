package nl.tudelft.ewi.build.docker;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildReference {
	
	private final DockerJob job;
	private final Identifiable containerId;
	private final Future<?> future;
	
	public BuildReference(DockerJob job, Identifiable containerId, Future<?> future) {
		this.job = job;
		this.containerId = containerId;
		this.future = future;
	}
	
	public DockerJob getJob() {
		return job;
	}
	
	public Identifiable getContainerId() {
		return containerId;
	}
	
	public void awaitTermination() {
		try {
			future.get();
		}
		catch (InterruptedException | ExecutionException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public boolean terminate() {
		return future.cancel(true);
	}
	
}