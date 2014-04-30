package nl.tudelft.ewi.build.jaxrs;

import java.util.UUID;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.tudelft.ewi.build.builds.BuildManager;
import nl.tudelft.ewi.build.jaxrs.filters.RequireAuthentication;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;

@Path("api/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BuildsResource {

	private final BuildManager manager;

	@Inject
	public BuildsResource(BuildManager manager) {
		this.manager = manager;
	}

	@POST
	@RequireAuthentication
	public Response onBuildRequest(@Valid BuildRequest buildRequest) {
		UUID id = manager.schedule(buildRequest);

		if (id == null) {
			return Response.status(Status.CONFLICT)
				.entity("Server cannot accept build request.")
				.build();
		}

		return Response.ok()
			.build();
	}

	@DELETE
	@RequireAuthentication
	public void killBuild(UUID buildId) {
		manager.killBuild(buildId);
	}

}
