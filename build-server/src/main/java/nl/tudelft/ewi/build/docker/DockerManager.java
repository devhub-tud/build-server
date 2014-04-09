package nl.tudelft.ewi.build.docker;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.Config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Slf4j
@Singleton
public class DockerManager {

	private final ScheduledThreadPoolExecutor executor;
	private final Config config;

	@Inject
	public DockerManager(Config config) {
		this.executor = new ScheduledThreadPoolExecutor(config.getMaximumConcurrentJobs() * 2);
		this.config = config;
	}
	
	public void run(final Logger logger, DockerJob job) {
		final String host = config.getDockerHost();
		final Identifiable container = create(host, job);
		start(host, container, job);
		logger.onStart();
		
		final Future<?> logFuture = fetchLog(host, container, logger);
		
		executor.submit(new Runnable() {
			@Override
			public void run() {
				StatusCode code = awaitTermination(host, container);
				logFuture.cancel(true);
				
				stop(host, container);
				delete(host, container);
				logger.onClose(code.getStatusCode());
			}
		});
	}
	
	private Identifiable create(final String host, DockerJob job) {
		Map<String, Object> volumes = Maps.newHashMap();
		if (job.getMounts() != null) {
			for (String mount : job.getMounts().values()) {
				volumes.put(mount, ImmutableMap.<String, Object>of());
			}
		}
		
		Container container = new Container()
			.setTty(true)
			.setCmd(CommandParser.parse(job.getCommand()))
			.setWorkingDir(job.getWorkingDirectory())
			.setVolumes(volumes)
			.setImage(job.getImage());
		
		Client client = null;
		try { 
			client = ClientBuilder.newClient();
			log.debug("Creating container: {}", container);
			return client.target(host + "/containers/create")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.json(container), Identifiable.class);
		}
		finally {
			if (client != null) {
				client.close();
			}
		}
	}
	
	private void start(final String host, final Identifiable container, final DockerJob job) {
		Client client = null;
		try { 
			client = ClientBuilder.newClient();
			List<String> mounts = Lists.newArrayList();
			if (job.getMounts() != null) {
				for (Entry<String, String> mount : job.getMounts().entrySet()) {
					mounts.add(mount.getKey() + ":" + mount.getValue() + ":rw");
				}
			}
			
			ContainerStart start = new ContainerStart()
				.setBinds(mounts)
				.setLxcConf(Lists.newArrayList(new LxcConf("lxc.utsname", "docker")));
	
			log.debug("Starting container: {} -> {}", container.getId(), start);
			client.target(host + "/containers/" + container.getId() + "/start")
					.request(MediaType.APPLICATION_JSON)
					.accept(MediaType.TEXT_PLAIN)
					.post(Entity.json(start));
		}
		finally {
			if (client != null) {
				client.close();
			}
		}
	}
	
	private Future<?> fetchLog(final String host, final Identifiable container, final Logger collector) {
		return executor.submit(new Runnable() {
			@Override
			public void run() {
				Client client = null;
				try { 
					client = ClientBuilder.newClient();
					log.debug("Streaming log from container: {}", container.getId());
					
					String url = "/containers/" + container.getId() + "/attach?logs=1&stream=1&stdout=1&stderr=1";
					final InputStream logs = client.target(host + url)
						.request()
						.accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
						.post(null, InputStream.class);
			
					try (InputStreamReader reader = new InputStreamReader(logs)) {
						boolean finished = false;
						StringBuilder builder = new StringBuilder();
						while (!finished) {
							int i = reader.read();
							if (i == -1) {
								collector.onNextLine(builder.toString());
								break;
							}
							
							char c = (char) i;
							if (c == '\n' || finished) {
								collector.onNextLine(builder.toString());
								builder.delete(0, builder.length() + 1);
							}
							else if (c != '\r') {
								builder.append(c);
							}
						}
					}
					catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
				finally {
					if (client != null) {
						client.close();
					}
				}
			}
		});
	}
	
	private StatusCode awaitTermination(String host, Identifiable container) {
		log.debug("Awaiting termination of container: {}", container.getId());
		
		StatusCode status;
		while (true) {
			status = getStatus(host, container);
			Integer statusCode = status.getStatusCode();
			if (statusCode != null) {
				break;
			}
			
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				log.error(e.getMessage(), e);
			}
		}

		log.debug("Container: {} terminated with status: {}", container.getId(), status);
		return status;
	}
	
	private StatusCode getStatus(final String host, final Identifiable container) {
		Client client = null;
		try { 
			client = ClientBuilder.newClient();
			log.debug("Retrieving status of container: {}", container.getId());
			return client.target(host + "/containers/" + container.getId() + "/wait")
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.post(null, StatusCode.class);
		}
		finally {
			if (client != null) {
				client.close();
			}
		}
	}
	
	private void stop(final String host, final Identifiable container) {
		Client client = null;
		try { 
			client = ClientBuilder.newClient();
			log.debug("Stopping container: {}", container.getId());
			client.target(host + "/containers/" + container.getId() + "/stop?t=5")
				.request(MediaType.APPLICATION_JSON)
				.accept(MediaType.TEXT_PLAIN)
				.post(null);
		}
		finally {
			if (client != null) {
				client.close();
			}
		}
	}
	
	private void delete(String host, Identifiable container) {
		Client client = null;
		try { 
			client = ClientBuilder.newClient();
			log.debug("Removing container: {}", container.getId());
			client.target(host + "/containers/" + container.getId() + "?v=1")
				.request()
				.delete();
		}
		finally {
			if (client != null) {
				client.close();
			}
		}
	}
	
	public static interface Logger {
		void onStart();
		void onNextLine(String line);
		void onClose(int statusCode);
	}
	
}
