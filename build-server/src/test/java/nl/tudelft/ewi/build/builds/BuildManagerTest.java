package nl.tudelft.ewi.build.builds;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.docker.DockerManager;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;
import nl.tudelft.ewi.build.jaxrs.models.GitSource;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BuildManagerTest {

	public static final int CONCURRENT_JOBS = 3;
	public static final int JOB_DURATION = 250;

	@Mock private Config config;

	private DockerManager dockerManager;
	private BuildManager manager;

	@Before
	public void setUp() {
		when(config.getMaximumConcurrentJobs()).thenReturn(CONCURRENT_JOBS);
		when(config.getWorkingDirectory()).thenReturn("/workspace");
		
		dockerManager = new MockedDockerManager(CONCURRENT_JOBS, JOB_DURATION);
		manager = new BuildManager(dockerManager, config);
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
	public void testThatJobIsRejectedWhenAtCapacity() throws InterruptedException {
		for (int i = 0; i < CONCURRENT_JOBS; i++) {
			manager.schedule(createRequest());
		}
		assertNull(manager.schedule(createRequest()));
	}

	@Test
	public void testThatJobCanBeScheduledWhenCapacityIsRestored() throws InterruptedException {
		for (int i = 0; i < CONCURRENT_JOBS; i++) {
			manager.schedule(createRequest());
		}

		Thread.sleep(10000);
		assertNotNull(manager.schedule(createRequest()));
	}

	@Test
	public void testThatJobCanBeScheduledWhenCapacityIsRestoredThroughTermination() throws InterruptedException {
		for (int i = 0; i < CONCURRENT_JOBS - 1; i++) {
			manager.schedule(createRequest());
		}
		
		UUID scheduled = manager.schedule(createRequest());
		assertNotNull(scheduled);
		assertTrue(manager.killBuild(scheduled));
		
		Thread.sleep(100);
		assertNotNull(scheduled);
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
