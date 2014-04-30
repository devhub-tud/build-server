package nl.tudelft.ewi.build.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult.Status;
import nl.tudelft.ewi.build.jaxrs.models.GitSource;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;

import org.jboss.resteasy.util.Base64;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.collect.Lists;

@Slf4j
public class MockedBuildServerBackend implements BuildServerBackend {

	public static void main(String[] args) throws InterruptedException {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		GitSource source = new GitSource();
		MavenBuildInstruction instruction = new MavenBuildInstruction();

		BuildRequest request = new BuildRequest();
		request.setSource(source);
		request.setInstruction(instruction);
		request.setCallbackUrl("http://localhost:9999/callback");

		BuildResult result = new BuildResult();
		result.setStatus(Status.SUCCEEDED);
		result.setLogLines(Lists.<String> newArrayList());

		MockedBuildServerBackend backend = new MockedBuildServerBackend("hello", "world");
		backend.setBuildDuration(5000);
		backend.setBuildResult(result);
		backend.offerBuildRequest(request);

		backend.shutdown();
	}

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final AtomicBoolean running = new AtomicBoolean(false);

	private final String user;
	private final String pass;

	private BuildResult result;
	private long milliseconds;

	public MockedBuildServerBackend(String user, String pass) {
		this.user = user;
		this.pass = pass;
	}

	public void setBuildResult(BuildResult result) {
		this.result = result;
	}

	public void setBuildDuration(long milliseconds) {
		this.milliseconds = milliseconds;
	}

	@SneakyThrows
	public void shutdown() {
		executor.shutdown();
		executor.awaitTermination(2, TimeUnit.MINUTES);
	}

	@Override
	public boolean offerBuildRequest(final BuildRequest request) {
		log.info("Offering build request...");
		synchronized (running) {
			if (running.compareAndSet(false, true)) {
				log.info("Build request accepted!");
				executor.submit(new BuildRunner(request));
				return true;
			}
		}
		log.info("Build request rejected!");
		return false;
	}

	private class BuildRunner implements Runnable {

		private final BuildRequest request;

		private BuildRunner(BuildRequest request) {
			this.request = request;
		}

		@Override
		@SneakyThrows
		public void run() {
			log.info("Executing build request...");
			Thread.sleep(milliseconds);

			log.info("Broadcasting build result...");

			Client client = null;
			try {
				client = ClientBuilder.newClient();
				client.target(request.getCallbackUrl())
					.request()
					.header("Authorization", "Basic " + Base64.encodeBytes((user + ":" + pass).getBytes()))
					.post(Entity.json(result), Response.class);

				log.info("Broadcasted build result!");
			}
			catch (Throwable e) {
				log.warn("Broadcast failed: " + e.getMessage(), e);
			}
			finally {
				if (client != null) {
					client.close();
				}

				synchronized (running) {
					running.set(false);
				}
			}
		}

	}

}
