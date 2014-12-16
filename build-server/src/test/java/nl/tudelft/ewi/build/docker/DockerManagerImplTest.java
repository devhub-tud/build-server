package nl.tudelft.ewi.build.docker;

import com.google.common.base.Joiner;
import com.spotify.docker.client.DockerCertificateException;

import nl.tudelft.ewi.build.SimpleConfig;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class DockerManagerImplTest {

	private DockerManager manager;
	
	@Before
	public void setUp() throws DockerCertificateException {
		Assume.assumeTrue("Skipping Docker related tests. If these tests should run set VM argument: "
				+ "-Ddocker-tests.run=true", "true".equalsIgnoreCase(System.getProperty("docker-tests.run")));
		
		SimpleConfig config = new SimpleConfig();
		this.manager = new DockerManagerImpl(config);
	}
	
	@Test
	public void testActiveJobsReturnsZeroWhenNoJobsScheduled() {
		Assert.assertEquals(0, manager.getActiveJobs());
	}
	
	@Test
	public void testStartingContainer() {
		DefaultLogger logger = new DefaultLogger();
		DockerJob job = new DockerJob();
		job.setCommand("whoami");
		job.setImage("java-maven");
		
		BuildReference build = manager.run(logger, job);
		build.awaitTermination();
		
		Assert.assertEquals("root", Joiner.on("").join(logger.getLogLines()));
	}
	
	@Test
	public void testTerminatingRunningContainer() throws InterruptedException {
		DefaultLogger logger = new DefaultLogger();
		DockerJob job = new DockerJob();
		job.setCommand("sleep 100");
		job.setImage("java-maven");
		
		BuildReference build = manager.run(logger, job);
		Assert.assertTrue(build.terminate());
	}
	
}
