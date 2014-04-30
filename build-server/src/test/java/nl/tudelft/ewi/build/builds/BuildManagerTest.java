package nl.tudelft.ewi.build.builds;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import lombok.SneakyThrows;
import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.docker.DockerJob;
import nl.tudelft.ewi.build.docker.DockerManager;
import nl.tudelft.ewi.build.docker.DockerManager.Logger;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class BuildManagerTest {

	public static final int CONCURRENT_JOBS = 3;
	public static final int JOB_DURATION = 500;

	@Mock
	private Config config;

	@Mock
	private DockerManager dockerManager;

	private BuildManager manager;

	@Before
	public void setUp() {
		when(config.getMaximumConcurrentJobs()).thenReturn(CONCURRENT_JOBS);

		doAnswer(new Answer<Void>() {
			@Override
			@SneakyThrows
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Logger logger = (Logger) invocation.getArguments()[0];
				Thread.sleep(JOB_DURATION);
				logger.onClose(0);
				return null;
			}
		}).when(dockerManager)
			.run(any(Logger.class), any(DockerJob.class));

		manager = new BuildManager(dockerManager, config);
	}

	@Test
	public void testThatWeCanScheduleSingleJob() {
		assertNotNull(manager.schedule(new BuildRequest()));
	}

	@Test
	public void testThatWeCanScheduleMultipleJobs() {
		for (int i = 0; i < CONCURRENT_JOBS - 1; i++) {
			manager.schedule(new BuildRequest());
		}
		assertNotNull(manager.schedule(new BuildRequest()));
	}

	@Test
	public void testThatJobIsRejectedWhenAtCapacity() {
		for (int i = 0; i < CONCURRENT_JOBS; i++) {
			manager.schedule(new BuildRequest());
		}
		assertNull(manager.schedule(new BuildRequest()));
	}

	@Test
	public void testThatJobCanBeScheduledWhenCapacityIsRestored() throws InterruptedException {
		for (int i = 0; i < CONCURRENT_JOBS; i++) {
			manager.schedule(new BuildRequest());
		}

		Thread.sleep(JOB_DURATION + 10);
		assertNotNull(manager.schedule(new BuildRequest()));
	}

	@Test
	public void testThatJobCanBeScheduledWhenCapacityIsRestoredThroughTermination() throws InterruptedException {
		for (int i = 0; i < CONCURRENT_JOBS - 1; i++) {
			manager.schedule(new BuildRequest());
		}

		assertTrue(manager.killBuild(manager.schedule(new BuildRequest())));

		Thread.sleep(100);
		assertNotNull(manager.schedule(new BuildRequest()));
	}

}
