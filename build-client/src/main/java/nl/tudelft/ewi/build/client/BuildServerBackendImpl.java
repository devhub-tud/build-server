package nl.tudelft.ewi.build.client;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;

import nl.tudelft.ewi.build.jaxrs.models.Version;
import org.jboss.resteasy.util.Base64;

/**
 * This class allows you query and manipulate builds on the build-server.
 */
@Slf4j
public class BuildServerBackendImpl extends Backend implements BuildServerBackend {

	private static final String BASE_PATH = "/api/builds";
	
	private final String clientId;
	private final String clientSecret;

	public BuildServerBackendImpl(String host, String clientId, String clientSecret) {
		super(host);
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	/**
	 * This method offers a new {@link BuildRequest} to the build-server.
	 * 
	 * @param buildRequest
	 *            The new {@link BuildRequest} to offer to the build-server.
	 * @return True if the build-server accepted the offered {@link BuildRequest} or false otherwise.
	 */
	@Override
	public boolean offerBuildRequest(final BuildRequest buildRequest) {
		return perform(new Request<Boolean>() {
			@Override
			public Boolean perform(Client client) {
				try {
					String authKey = clientId + ":" + clientSecret;
					String authHash = Base64.encodeBytes(authKey.getBytes());
					
					Response response = client.target(createUrl(BASE_PATH))
							.request(MediaType.APPLICATION_JSON)
							.header("Authorization", "Basic " + authHash)
							.post(Entity.json(buildRequest), Response.class);
					
					int statusCode = response.getStatus();
					Status status = Status.fromStatusCode(statusCode);
					return status == Status.OK;
				}
				catch (ProcessingException e) {
					log.warn(e.getMessage(), e);
					return false;
				}
			}
		});
	}

	@Override
	public Version version() {
		return perform(new Request<Version>() {
			@Override
			public Version perform(Client client) {
				return client.target("api")
					.path("version")
					.request()
					.get(Version.class);
			}
		});
	}


}
