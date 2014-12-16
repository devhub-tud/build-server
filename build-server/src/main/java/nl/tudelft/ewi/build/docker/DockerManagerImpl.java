package nl.tudelft.ewi.build.docker;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerCertificates;

import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.Config;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

@Slf4j
public class DockerManagerImpl implements DockerManager {

	private final ExecutorService executor;
	private final Config config;
	private final ClientBuilder clientBuilder;

	@Inject
	public DockerManagerImpl(final Config config) throws DockerCertificateException {
		int poolSize = config.getMaximumConcurrentJobs() * 2;
		this.executor = Executors.newScheduledThreadPool(poolSize);
		this.config = config;
		
		Path certificateFolder = new File(config.getCertificateDirectory()).toPath();
		DockerCertificates certificates = new DockerCertificates(certificateFolder);
		
		this.clientBuilder = new ResteasyClientBuilder()
				.hostnameVerifier(certificates.hostnameVerifier())
				.sslContext(certificates.sslContext());
	}
	
	@Override
	public BuildReference run(final Logger logger, final DockerJob job) {
		final String host = config.getDockerHost();
		final Identifiable containerId = create(host, job);

		final Future<?> future = executor.submit(new Runnable() {
			@Override
			public void run() {
				start(host, containerId, job);
				logger.onStart();

				Future<?> logFuture = fetchLog(host, containerId, logger);
				StatusCode code = awaitTermination(host, containerId);
				logFuture.cancel(true);

				stopAndDelete(host, containerId);
				logger.onClose(code.getStatusCode());
			}
		});
		
		return new BuildReference(job, containerId, new Future<Object>() {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				if (future.cancel(mayInterruptIfRunning)) {
					log.warn("Terminating container: {} because it was cancelled.", containerId);
					stopAndDelete(host, containerId);
					log.warn("Container: {} was terminated forcefully.", containerId);
					return true;
				}
				return false;
			}

			@Override
			public boolean isCancelled() {
				return future.isCancelled();
			}

			@Override
			public boolean isDone() {
				return future.isDone();
			}

			@Override
			public Object get() throws InterruptedException, ExecutionException {
				return future.get();
			}

			@Override
			public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				return future.get(timeout, unit);
			}
		});
	}
	
	@Override
	public void terminate(Identifiable container) {
		String host = config.getDockerHost();
		stopAndDelete(host, container);
	}

	@Override
	public int getActiveJobs() {
		int counter = 0;
		for (Container container : getContainers().values()) {
			if (!Strings.emptyToNull(container.getStatus()).startsWith("Exit ")) {
				return counter++;
			}
		}
		return counter;
	}

	@Override
	public void buildImage(String name, String dockerFileContents, ImageBuildObserver observer) throws IOException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(name));
		Preconditions.checkArgument(!Strings.isNullOrEmpty(dockerFileContents));
		
		name = URLEncoder.encode(name, "UTF-8");
		
		File tempDir = Files.createTempDir();
		File archive = new File(tempDir, "image.tar");
		File dockerFile = new File(tempDir, "Dockerfile");
		
		FileWriter writer = new FileWriter(dockerFile);
		writer.write(dockerFileContents);
		writer.flush();
		writer.close();
		
		try (TarOutputStream out = new TarOutputStream(new BufferedOutputStream(new FileOutputStream(archive)))) {
			out.putNextEntry(new TarEntry(dockerFile, dockerFile.getName()));
			
			int count;
			byte data[] = new byte[2048];
			try (BufferedInputStream origin = new BufferedInputStream(new FileInputStream(dockerFile))) {
				while((count = origin.read(data)) != -1) {
					out.write(data, 0, count);
				}
			}
			out.flush();
		}

		Client client = null;
		try {
			client = clientBuilder.build();
			log.debug("Requesting image to be built...");
			InputStream output = client.target(config.getDockerHost() + "/build?t=" + name + "&nocache=true&forcerm=true")
				.request("application/tar")
				.post(Entity.entity(archive, "application/tar"), InputStream.class);
			
			ObjectMapper mapper = new ObjectMapper();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(output))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("{\"stream\":")) {
						observer.onMessage(mapper.readValue(line, Stream.class).getStream());
					}
					else if (line.startsWith("{\"error\":")) {
						observer.onError(mapper.readValue(line, Error.class).getErrorDetail().getMessage());
					}
				}
			}
		}
		finally {
			observer.onCompleted();
			if (client != null) {
				client.close();
			}
			
			archive.delete();
			dockerFile.delete();
			tempDir.delete();
		}
	}
	
	private Map<Identifiable, Container> getContainers() {
		Client client = null;
		try {
			client = clientBuilder.build();
			log.debug("Listing containers...");
			List<Container> containers = client.target(config.getDockerHost() + "/containers/json")
				.request(MediaType.APPLICATION_JSON)
				.get(new GenericType<List<Container>>() { });
			
			Map<Identifiable, Container> mapping = Maps.newLinkedHashMap();
			for (Container container : containers) {
				Identifiable identifiable = new Identifiable();
				identifiable.setId(container.getId());
				mapping.put(identifiable, container);
			}
			return mapping;
		}
		finally {
			if (client != null) {
				client.close();
			}
		}
	}

	private Identifiable create(final String host, DockerJob job) {
		List<String> mounts = Lists.newArrayList();
		if (job.getMounts() != null) {
			for (Entry<String, String> mount : job.getMounts()
				.entrySet()) {
				mounts.add(mount.getKey() + ":" + mount.getValue());// + ":rw");
			}
		}

		Container container = new Container().setTty(true)
			.setCmd(CommandParser.parse(job.getCommand()))
			.setWorkingDir(job.getWorkingDirectory())
			.setVolumes(mounts)
			.setImage(job.getImage());

		Client client = null;
		try {
			client = clientBuilder.build();
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
			client = clientBuilder.build();
			List<String> mounts = Lists.newArrayList();
			if (job.getMounts() != null) {
				for (Entry<String, String> mount : job.getMounts()
					.entrySet()) {
					mounts.add(mount.getKey() + ":" + mount.getValue());// + ":rw");
				}
			}

			ContainerStart start = new ContainerStart().setBinds(mounts)
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
					client = clientBuilder.build();
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
	
	private boolean isStopped(String host, Identifiable identifiable) {
		try {
			Map<Identifiable, Container> containers = getContainers();
			if (containers.containsKey(identifiable)) {
				Container container = containers.get(identifiable);
				return Strings.nullToEmpty(container.getStatus()).startsWith("Exit ");
			}
			return true;
		}
		catch (InternalServerErrorException | NotFoundException e) {
			log.warn(e.getMessage(), e);
			return true;
		}
	}

	private StatusCode getStatus(final String host, final Identifiable container) {
		Client client = null;
		try {
			client = clientBuilder.build();
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
	
	private boolean exists(String host, Identifiable container) {
		try {
			return getContainers().containsKey(container);
		}
		catch (InternalServerErrorException | NotFoundException e) {
			return false;
		}
	}
	
	private void stopAndDelete(String host, Identifiable container) {
		try {
			log.debug("Attempting to stop container: {}", container);
			do {
				stop(host, container);
				waitFor(1000);
			}
			while (!isStopped(host, container));
		}
		catch (ClientErrorException e) {
			log.warn(e.getMessage(), e);
		}

		try {
			log.debug("Attempting to delete container: {}", container);
			do {
				delete(host, container);
				waitFor(1000);
			}
			while (exists(host, container));
		}
		catch (ClientErrorException e) {
			log.warn(e.getMessage(), e);
		}
	}

	private void stop(final String host, final Identifiable container) {
		Client client = null;
		try {
			client = clientBuilder.build();
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
			client = clientBuilder.build();
			log.debug("Removing container: {}", container.getId());
			client.target(host + "/containers/" + container.getId() + "?v=1&force=1")
				.request()
				.delete();
		}
		finally {
			if (client != null) {
				client.close();
			}
		}
	}
	
	private void waitFor(int millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			log.warn(e.getMessage(), e);
		}
	}
	
}
