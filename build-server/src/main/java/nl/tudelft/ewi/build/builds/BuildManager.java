package nl.tudelft.ewi.build.builds;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;

import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.extensions.instructions.BuildInstructionInterpreter;
import nl.tudelft.ewi.build.extensions.instructions.BuildInstructionInterpreterRegistry;
import nl.tudelft.ewi.build.extensions.staging.StagingDirectoryPreparer;
import nl.tudelft.ewi.build.extensions.staging.StagingDirectoryPreparerRegistry;
import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult;
import nl.tudelft.ewi.build.jaxrs.models.Source;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult.Status;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.DockerClient.AttachParameter;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.HostConfig;

@Slf4j
@Singleton
public class BuildManager extends AbstractLifeCycleListener implements LifeCycle.Listener {

	private final static String WORK_DIR = "/workdir";

	private final Config config;
	private final DockerClient dockerClient;
	private final StagingDirectoryPreparerRegistry stagingDirectoryPreparerRegistry;
	private final BuildInstructionInterpreterRegistry buildInstructionInterpreterRegistry;
	private final Map<UUID, ListenableFuture<BuildResult>> builds;
	private final ListeningScheduledExecutorService executor;

	@Inject
	public BuildManager(
			final Config config,
			final DockerClient dockerClient,
			final StagingDirectoryPreparerRegistry stagingDirectoryPreparerRegistry,
			final BuildInstructionInterpreterRegistry buildInstructionInterpreterRegistry) {
		
		super();
		this.config = config;
		this.dockerClient = dockerClient;
		this.stagingDirectoryPreparerRegistry = stagingDirectoryPreparerRegistry;
		this.buildInstructionInterpreterRegistry = buildInstructionInterpreterRegistry;
		this.builds = Maps.newHashMap();
		this.executor = MoreExecutors.listeningDecorator(Executors
				.newScheduledThreadPool(2 * config.getMaximumConcurrentJobs()));
	}
	
	/**
	 * Schedule a new build
	 * @param buildRequest {@link BuildRequest} object that describes the build
	 * @return {@link UUID} which identifies the build
	 * @see #killBuild(UUID)
	 */
	public Build schedule(final BuildRequest buildRequest) {
		if(builds.size() >= config.getMaximumConcurrentJobs()) {
			return null;
		}
		
		return new Build(buildRequest);
	}

	/**
	 * Kill a scheduled build
	 * @param uuid {@link UUID} that identifies the build
	 * @see #schedule(BuildRequest)
	 */
	public void killBuild(final UUID uuid) {
		final Future<BuildResult> build = builds.remove(uuid);
		if (build == null)
			throw new IllegalArgumentException("Build does not exist!");
		else
			build.cancel(true);
	}
	
	/**
	 * Get the {@link Future} for a certain build
	 * @param uuid {@link UUID} that identifies the build
	 * @return the {@link Future} for the build
	 * @see #schedule(BuildRequest)
	 */
	public ListenableFuture<BuildResult> getBuild(final UUID uuid) {
		return builds.get(uuid);
	}

	@Override
	public void lifeCycleStopping(LifeCycle event) {
		executor.shutdown();
	}

	/**
	 * The {@link Build} class forms the {@link Future} for the
	 * {@link BuildRequest}, which completes even if the build fails, or the
	 * container exits abnormally due to an error, timeout or cancellation. In
	 * this case the build {@link Status} is set to {@code FAILED}.
	 * 
	 * @author Jan-Willem Gmelig Meyling
	 *
	 */
	public class Build extends AbstractFuture<BuildResult>
			implements ListenableFuture<BuildResult>, Runnable {

		private final BuildRequest buildRequest;
		private final BuildResult buildResult;
		private final Logger logger;
		private final BuildTask buildTask;

		Build(final BuildRequest buildRequest) {
			this.buildRequest = buildRequest;
			this.buildResult = new BuildResult();
			this.logger = new BuildResultLogger(buildResult);
			this.buildTask = new BuildTask(new BuildRunner(logger, buildRequest));
			start();
		}

		private final void start() {
			executor.execute(buildTask);
			executor.execute(this);
			builds.put(getUUID(), this);
		}

		@Override
		public void run() {
			try {
				Integer timeout = buildRequest.getTimeout();
				ContainerExit exit = timeout == null || timeout == 0 ? buildTask.get()
						: buildTask.get(timeout, TimeUnit.SECONDS);
				Status status = exit.statusCode() == 0 ? Status.SUCCEEDED
						: Status.FAILED;
				buildResult.setStatus(status);
			}
			catch (TimeoutException e) {
				buildTask.cancel(true);
				logger.println("[FATAL] Build timed out!");
				buildResult.setStatus(Status.FAILED);
				log.info("Build timed out {}", getUUID());
			}
			catch (CancellationException e) {
				logger.println("[FATAL] Build was cancelled!");
				buildResult.setStatus(Status.FAILED);
				log.info("Build cancelled " + getUUID());
			}
			catch (Throwable t) {
				buildResult.setStatus(Status.FAILED);
				logger.println("[FATAL] An exception occured during the build!");
				log.warn("Build task failed " + getUUID(), t);
			}
			finally {
				logger.close();
				builds.remove(getUUID());
			}
			set(buildResult);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if(super.cancel(mayInterruptIfRunning)) {
				// Cancel the build as well
				buildTask.cancel(mayInterruptIfRunning);
				return true;
			}
			return false;
		}

		public UUID getUUID() {
			return buildTask.getUUID();
		}

	}

	/**
	 * The {@link BuildTask} wraps a {@link BuildRunner} in a {@link FutureTask}
	 * , to enforce killing the container on cancellation and removing the
	 * container after the {@link Future} is done.
	 * 
	 * @author Jan-Willem Gmelig Meyling
	 *
	 */
	class BuildTask extends FutureTask<ContainerExit> {

		private final BuildRunner buildRunner;

		BuildTask(final BuildRunner buildRunner) {
			super(buildRunner);
			this.buildRunner = buildRunner;
		}

		@Override
	    protected void done() {
			if(isCancelled()) {
				buildRunner.kill();
			}
			buildRunner.remove();
			builds.remove(getUUID());
		}
		
		public UUID getUUID() {
			return buildRunner.getUUID();
		}
		
	}

	/**
	 * The {@link BuildRunner} is a {@link Callable} responsible for creating
	 * and watching the Docker container.
	 * 
	 * @author Jan-Willem Gmelig Meyling
	 *
	 */
	class BuildRunner implements Callable<ContainerExit> {

		private final UUID uuid;
		private final Logger logger;
		private final BuildRequest buildRequest;
		private final AtomicReference<File> stagingDirectoryReference;
		private final AtomicReference<String> containerId;
		private final AtomicReference<Boolean> started;

		BuildRunner(final Logger logger, final BuildRequest buildRequest) {
			this.stagingDirectoryReference = new AtomicReference<File>();
			this.containerId = new AtomicReference<String>();
			this.started = new AtomicReference<Boolean>(false);
			this.buildRequest = buildRequest;
			this.uuid = UUID.randomUUID();
			this.logger = logger;
		}
		
		@Override
		public ContainerExit call() throws Exception {
			if(!started.compareAndSet(false, true))
				throw new IllegalStateException("DockerRunner is already running!");

			File stagingDirectory = createStagingDirectory();
			stagingDirectoryReference.set(stagingDirectory);
			prepareStagingDirectory(stagingDirectory);

			String volume = String.format("%s:%s", stagingDirectory, WORK_DIR);

			BuildInstructionInterpreter<BuildInstruction> buildInstructionInterpreter =
					getBuildIntstructionInterpreter();
			BuildInstruction buildInstruction = buildRequest.getInstruction();
			
			ContainerConfig.Builder configBuilder = ContainerConfig.builder()
					.image(buildInstructionInterpreter.getImage(buildInstruction))
					.cmd(buildInstructionInterpreter.getCommand(buildInstruction).split(" "))
					.user(config.getDockerUser())
					.volumes(volume)
					.workingDir(WORK_DIR);

			String id;

			try {
				log.info("Create container {}", config);
				ContainerCreation creation = dockerClient.createContainer(configBuilder.build(), uuid.toString());
				id = creation.id();
				containerId.set(id);
				log.info("Starting container {}", id);
				dockerClient.startContainer(id, HostConfig.builder().binds(volume).build());
			}
			catch (DockerException | InterruptedException e) {
				logger.println("[FATAL] Failed to provision build environment");
				throw e;
			}

			try(LogStream stream = dockerClient.attachContainer(id, AttachParameter.LOGS,
					AttachParameter.STDERR, AttachParameter.STDOUT, AttachParameter.STREAM)) {
				log.info("Attaching log for container {}", id);
				while(stream.hasNext() && !Thread.currentThread().isInterrupted())
					logger.consume(stream.next());
			}

			log.info("Waiting for container to terminate {}", id);
			return dockerClient.waitContainer(id);
		}

		/**
		 * Kill the Docker container. This is called when the Future was cancelled
		 * and thus the container did not exit yet.
		 */
		public void kill() {
			String id = containerId.get();
			if(id != null) {
				log.info("Trying to kill container {}", id);
				try {
					dockerClient.killContainer(id);
				}
				catch (DockerException | InterruptedException e) {
					log.warn("Failed to kill container " + id, e);
				}
			}
		}

		/**
		 * Remove the Docker container. This is called when the Future is either
		 * cancelled or completed. This also removes the staging directory that
		 * was shared with the container. 
		 */
		public void remove() {
			String id = containerId.get();
			if(id != null) {
				log.info("Trying to remove container {}", id);
				try {
					dockerClient.removeContainer(id, true);
				}
				catch (DockerException | InterruptedException e) {
					log.info("Failed to remove container " + id, e);
				}
			}
			
			removeStagingDirectory();
		}

		protected File createStagingDirectory() throws IOException {
			try {
				File stagingDirectory = new File(config.getStagingDirectory(), uuid.toString());
				log.info("Created staging directory: {}", stagingDirectory.getAbsolutePath());
				stagingDirectory.mkdirs();
				return stagingDirectory;
			}
			catch (Throwable e) {
				logger.println("[FATAL] Failed to allocate new working directory for build");
				throw new IOException(e);
			}
		}

		protected void removeStagingDirectory() {
			File dir = stagingDirectoryReference.get();
			if(dir != null && dir.exists()) {
				try {
					log.info("Removing staging directory {}", stagingDirectoryReference);
					FileUtils.deleteDirectory(dir);
				}
				catch (IOException e) {
					log.warn("Failed to cleanup staging directory " + stagingDirectoryReference, e);
				}
			}
		}

		@SuppressWarnings("unchecked")
		protected void prepareStagingDirectory(final File stagingDirectory) throws IOException {
			Source source = buildRequest.getSource();
			StagingDirectoryPreparer<Source> preparer = 
					(StagingDirectoryPreparer<Source>)
					stagingDirectoryPreparerRegistry.getStagingDirectoryPreparer(source.getClass());
			log.info("Preparing staging directory {} with source: {}", stagingDirectory, source);
			preparer.prepareStagingDirectory(source, logger, stagingDirectory);
		}

		@SuppressWarnings("unchecked")
		protected BuildInstructionInterpreter<BuildInstruction> getBuildIntstructionInterpreter() {
			BuildInstruction instruction = buildRequest.getInstruction();
			return (BuildInstructionInterpreter<BuildInstruction>) buildInstructionInterpreterRegistry
					.getBuildDecorator(instruction.getClass());
		}

		public UUID getUUID() {
			return uuid;
		}

	}

}
