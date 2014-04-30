package nl.tudelft.ewi.build.client;

import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;

public interface BuildServerBackend {

	/**
	 * This method offers a new {@link BuildRequest} to the build-server.
	 * 
	 * @param buildRequest
	 *            The new {@link BuildRequest} to offer to the build-server.
	 * @return True if the build-server accepted the offered {@link BuildRequest} or false otherwise.
	 */
	boolean offerBuildRequest(BuildRequest buildRequest);

}