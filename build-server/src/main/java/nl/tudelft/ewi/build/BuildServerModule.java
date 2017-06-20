package nl.tudelft.ewi.build;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.jaxrs.json.MappingModule;

import org.jboss.resteasy.plugins.guice.ext.JaxrsModule;
import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;
import org.reflections.Reflections;

@Slf4j
public class BuildServerModule extends AbstractModule {
	
	private final Config config;

	public BuildServerModule(Config config) {
		this.config = config;
	}

	@Override
	@SneakyThrows
	protected void configure() {
		install(new RequestScopeModule());
		install(new JaxrsModule());
		
		bind(Config.class).toInstance(config);
		bind(ObjectMapper.class).toProvider(new com.google.inject.Provider<ObjectMapper>() {
			@Override
			public ObjectMapper get() {
				ObjectMapper mapper = new ObjectMapper();
				mapper.registerModule(new MappingModule());
				return mapper;
			}
		});
		
		bind(DockerClient.class).toInstance(DefaultDockerClient.fromEnv()
				.readTimeoutMillis(0)
				.build());
		
		findResourcesWith(Path.class);
		findResourcesWith(Provider.class);
	}

	private void findResourcesWith(Class<? extends Annotation> ann) {
		Reflections reflections = new Reflections(getClass().getPackage().getName());
		for (Class<?> clasz : reflections.getTypesAnnotatedWith(ann)) {
			log.info("Registering resource {}", clasz);
			bind(clasz);
		}
	}

}
