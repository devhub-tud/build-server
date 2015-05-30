package nl.tudelft.ewi.build.builds;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.AttachParameter;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.MockedLogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.builds.BuildManager.Build;
import nl.tudelft.ewi.build.extensions.instructions.BuildInstructionInterpreterRegistry;
import nl.tudelft.ewi.build.extensions.instructions.MavenBuildInstructionInterpreter;
import nl.tudelft.ewi.build.extensions.staging.GitStagingDirectoryPreparer;
import nl.tudelft.ewi.build.extensions.staging.StagingDirectoryPreparerRegistry;
import nl.tudelft.ewi.build.jaxrs.json.MappingModule;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult.Status;
import nl.tudelft.ewi.build.jaxrs.models.GitSource;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class BuildManagerTest {

	public static final int CONCURRENT_JOBS = 3;

	@Mock private Config config;
	@Mock private DockerClient dockerClient;
	@InjectMocks private MavenBuildInstructionInterpreter mavenBuildInstructionInterpreter;
	@Mock private GitStagingDirectoryPreparer gitStagingDirectoryPreparer;
	
	private BuildManager manager;

	private static File stagingDirectory;

	@BeforeClass
	public static void createTempDir() {
		stagingDirectory = Files.createTempDir();
		stagingDirectory.deleteOnExit();
	}

	@Before
	public void setUp() throws DockerException, InterruptedException {
		when(config.getMaximumConcurrentJobs()).thenReturn(CONCURRENT_JOBS);
		when(config.getStagingDirectory()).thenReturn(stagingDirectory.getAbsolutePath());
		
		when(dockerClient.createContainer(Mockito.any(ContainerConfig.class), Mockito.anyString()))
				.thenReturn(new ContainerCreation(UUID.randomUUID().toString()));
		when(dockerClient.attachContainer(Mockito.anyString(), Mockito.<AttachParameter>anyVararg()))
				.thenReturn(new MockedLogStream());
		when(dockerClient.waitContainer(Mockito.anyString())).thenReturn(new ContainerExit(0));
		
		manager = new BuildManager(config, dockerClient,
				new StagingDirectoryPreparerRegistry(gitStagingDirectoryPreparer),
				new BuildInstructionInterpreterRegistry(mavenBuildInstructionInterpreter));
	}
	
	@After
	public void tearDown() {
		manager.lifeCycleStopping(null);
	}

	@Test
	public void testThatWeCanScheduleSingleJob() {
		assertNotNull(manager.schedule(createRequest()));
	}

	@Test
	public void testThatWeCanScheduleMultipleJobs() {
		for (int i = 0; i < CONCURRENT_JOBS - 1; i++) {
			manager.schedule(createRequest());
		}
		assertNotNull(manager.schedule(createRequest()));
	}

	@Test
	public void testThatJobIsRejectedWhenAtCapacity() throws InterruptedException, IOException {
		setGitPullDuration(200l);

		for (int i = 0; i < CONCURRENT_JOBS; i++) {
			manager.schedule(createRequest());
		}

		assertNull(manager.schedule(createRequest()));
	}

	private void setGitPullDuration(final long duration) throws IOException {
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				Thread.sleep(duration);
				return null;
			}
		}).when(gitStagingDirectoryPreparer)
				.prepareStagingDirectory(any(GitSource.class), any(Logger.class), eq(stagingDirectory));
	}

	@Test
	public void testThatJobCanBeScheduledWhenCapacityIsRestored() throws InterruptedException {
		UUID uuid = null;
		for (int i = 0; i < CONCURRENT_JOBS; i++) {
			uuid = manager.schedule(createRequest()).getUUID();
		}
		
		assertNotNull(uuid);
		manager.killBuild(uuid);
		assertNotNull(manager.schedule(createRequest()));
	}

	@Test
	public void testThatJobCanBeScheduledWhenCapacityIsRestoredThroughTermination() throws InterruptedException {
		for (int i = 0; i < CONCURRENT_JOBS - 1; i++) {
			manager.schedule(createRequest());
		}
		
		UUID scheduled = manager.schedule(createRequest()).getUUID();
		assertNotNull(scheduled);
		
		Thread.sleep(100);
		assertNotNull(scheduled);
	}
	
	@Test
	public void waitForABuild() throws InterruptedException, ExecutionException {
		Build result = manager.schedule(createRequest());
		log.info("Result : {}", result.get());
	}
	
	@Test(timeout=2000) // kill test after 2 seconds
	public void testBuildWithTimeout() throws DockerException, InterruptedException, ExecutionException {
		BuildRequest buildRequest = createRequest();
		buildRequest.setTimeout(1); // timeout 1 second
		setContainerExitDuration(20000l);

		Build future = manager.schedule(buildRequest);
		BuildResult buildResult = future.get();
		Assert.assertEquals(Status.FAILED, buildResult.getStatus());
		Assert.assertThat(buildResult.getLogLines(), Matchers.hasItem("[FATAL] Build timed out!"));
	}

	private void setContainerExitDuration(final long duration) throws DockerException, InterruptedException {
		when(dockerClient.waitContainer(Mockito.anyString())).then(new Answer<ContainerExit>() {

			@Override
			public ContainerExit answer(InvocationOnMock invocation)
					throws Throwable {
				Thread.sleep(duration); // sleep 20 seconds
				return new ContainerExit(0);
			}

		});
	}

	@Test
	public void testOldRequestSupport() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModules(new MappingModule());
		try(InputStream in = BuildManagerTest.class.getResourceAsStream("/build-requests/java-maven-old.json")) {
			BuildRequest request = objectMapper.readValue(in, BuildRequest.class);
			Build resultFuture = manager.schedule(request);
			BuildResult result = resultFuture.get();
			assertEquals(Status.SUCCEEDED, result.getStatus());
		}
	}

	@Test
	public void testNewRequestSupport() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModules(new MappingModule());
		try(InputStream in = BuildManagerTest.class.getResourceAsStream("/build-requests/java-maven-new.json")) {
			BuildRequest request = objectMapper.readValue(in, BuildRequest.class);
			Build resultFuture = manager.schedule(request);
			BuildResult result = resultFuture.get();
			assertEquals(Status.SUCCEEDED, result.getStatus());
		}
	}
	
	private BuildRequest createRequest() {
		GitSource source = new GitSource();
		source.setBranchName("master");
		source.setRepositoryUrl("https://github.com/devhub-tud/build-server.git");
		source.setCommitId("2625eaf9b476dd158ad5f9cad3d5137f3b111ea7");
		
		MavenBuildInstruction instruction = new MavenBuildInstruction();
		instruction.setPhases(new String[] { "package" });
		instruction.setWithDisplay(true);
		
		BuildRequest request = new BuildRequest();
		request.setSource(source);
		request.setInstruction(instruction);
		return request;
	}

}
