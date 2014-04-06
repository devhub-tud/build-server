package nl.tudelft.ewi.build.builds;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;

import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.docker.DefaultLogger;
import nl.tudelft.ewi.build.docker.DefaultLogger.OnClose;
import nl.tudelft.ewi.build.docker.DockerJob;
import nl.tudelft.ewi.build.docker.DockerManager;
import nl.tudelft.ewi.build.docker.DockerManager.Logger;
import nl.tudelft.ewi.build.extensions.instructions.BuildInstructionInterpreter;
import nl.tudelft.ewi.build.extensions.instructions.BuildInstructionInterpreterRegistry;
import nl.tudelft.ewi.build.extensions.staging.StagingDirectoryPreparer;
import nl.tudelft.ewi.build.extensions.staging.StagingDirectoryPreparerRegistry;
import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult;
import nl.tudelft.ewi.build.jaxrs.models.Source;

import org.jboss.resteasy.util.Base64;

import com.google.common.collect.ImmutableMap;

@Data
@Slf4j 
@Getter(AccessLevel.NONE)
class BuildRunner implements Runnable {
	
	private final DockerManager docker;
	private final Config config;
	private final BuildRequest request;
	private final UUID identifier;
	
	@Override
	public void run() {
		final DefaultLogger logger = new DefaultLogger(); 
		try {
			final File stagingDirectory = createStagingDirectory(logger);
			logger.onClose(new OnClose() {
				@Override
				public void onClose() {
					BuildResult result = createBuildResult(logger, stagingDirectory);
					broadcastResultThroughCallback(result);
				}
			});
			
			prepareStagingDirectory(logger, stagingDirectory);
			startDockerJob(logger, stagingDirectory.getAbsolutePath());
		}
		catch (Throwable e) {
			log.error(e.getMessage(), e);
			logger.onClose(-1);
		}
	}

	private File createStagingDirectory(Logger logger) throws IOException {
		File stagingDirectory = new File(config.getStagingDirectory(), identifier.toString());
		try {
			log.info("Created staging directory: {}", stagingDirectory.getAbsolutePath());
			stagingDirectory.mkdirs();
			return stagingDirectory;
		}
		catch (Throwable e) {
			logger.onNextLine("[FATAL] Failed to allocate new working directory for build");
			throw new IOException(e);
		}
	}

	private void startDockerJob(Logger logger, String stagingDirectory) throws Throwable {
		BuildInstruction instruction = request.getInstruction();
		BuildInstructionInterpreter<BuildInstruction> buildDecorator = createBuildDecorator();
		
		DockerJob job = new DockerJob();
		job.setCommand(buildDecorator.getCommand(instruction));
		job.setImage(buildDecorator.getImage(instruction));
		job.setWorkingDirectory(config.getWorkingDirectory());
		job.setMounts(ImmutableMap.of(stagingDirectory, config.getWorkingDirectory()));
		
		try {
			log.debug("Starting docker job: {}", instruction);
			docker.run(logger, job);
		}
		catch (Throwable e) {
			logger.onNextLine("[FATAL] Failed to provision new build environment");
			throw e;
		}
	}
	
	private void prepareStagingDirectory(DefaultLogger logger, File stagingDirectory) throws IOException {
		Source source = request.getSource();
		log.info("Preparing staging directory with source: {}", source);
		createStagingDirectoryPreparer().prepareStagingDirectory(source, logger, stagingDirectory);
	}

	private BuildResult createBuildResult(DefaultLogger logger, File stagingDirectory) {
		BuildInstruction instruction = request.getInstruction();
		log.info("Creating build result according to instruction: {}", instruction);
		return createBuildDecorator().createResult(instruction, logger, stagingDirectory);
	}
	
	@SuppressWarnings("unchecked")
	private BuildInstructionInterpreter<BuildInstruction> createBuildDecorator() {
		BuildInstruction instruction = request.getInstruction();
		BuildInstructionInterpreterRegistry registry = new BuildInstructionInterpreterRegistry();
		return (BuildInstructionInterpreter<BuildInstruction>) registry.getBuildDecorator(instruction.getClass());
	}
	
	@SuppressWarnings("unchecked")
	private StagingDirectoryPreparer<Source> createStagingDirectoryPreparer() {
		Source source = request.getSource();
		StagingDirectoryPreparerRegistry registry = new StagingDirectoryPreparerRegistry();
		return (StagingDirectoryPreparer<Source>) registry.getStagingDirectoryPreparer(source.getClass());
	}
	
	private void broadcastResultThroughCallback(BuildResult result) {
		log.info("Sending build results to callback URL: " + request.getCallbackUrl());
		@Cleanup Client client = ClientBuilder.newClient();
		prepareCallback(client).post(Entity.json(result));
	}

	private Builder prepareCallback(Client client) {
		String userPass = config.getClientId() + ":" + config.getClientSecret();
		String authorization = "Basic " + Base64.encodeBytes(userPass.getBytes());
		return client.target(request.getCallbackUrl())
				.request()
				.header("Authorization", authorization);
	}
	
}