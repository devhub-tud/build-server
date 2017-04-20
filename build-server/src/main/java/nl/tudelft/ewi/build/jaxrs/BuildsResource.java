package nl.tudelft.ewi.build.jaxrs;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.util.Base64;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import lombok.extern.slf4j.Slf4j;

import nl.tudelft.ewi.build.Config;
import nl.tudelft.ewi.build.builds.BuildManager;
import nl.tudelft.ewi.build.builds.BuildManager.Build;
import nl.tudelft.ewi.build.jaxrs.filters.RequireAuthentication;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult;

@Slf4j
@Path("api/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BuildsResource {

	private final BuildManager manager;
	private final ExecutorService executor = Executors
			.newSingleThreadExecutor();
	private final Config config;

	@Inject
	public BuildsResource(final BuildManager manager, final Config config) {
		this.manager = manager;
		this.config = config;
	}

	@POST
	@RequireAuthentication
	public Response onBuildRequest(@Valid final BuildRequest buildRequest) {
		Build build = null;
		try {
			build = manager.schedule(buildRequest);

			if (build != null) {
				Futures.addCallback(build, new FutureCallback<BuildResult>() {

					@Override
					public void onSuccess(BuildResult result) {
						if (Strings.isNullOrEmpty(buildRequest.getCallbackUrl())) {
							return;
						}

						log.info("Returning build results to callback URL: {}",
							buildRequest.getCallbackUrl());
						for (int i = 0; i <= 4; i++) {
							Client client = new ResteasyClientBuilder().build();
							try {
								Response response = prepareCallback(client).post(
									Entity.json(result));
								StatusType statusInfo = response.getStatusInfo();
								if (statusInfo.getStatusCode() >= 200
									&& statusInfo.getStatusCode() < 300) {
									log.info(
										"Build result successfully returned to: {}",
										buildRequest.getCallbackUrl());
									return;
								}
								log.warn(
									"Could not return build result to: {}, status was: {} - {}",
									buildRequest.getCallbackUrl(),
									response.getStatus(),
									statusInfo.getReasonPhrase());
							} catch (Throwable e) {
								log.warn(e.getMessage(), e);
							} finally {
								if (client != null) {
									client.close();
								}
							}

							// Exponential backoff.
							if (i < 4) {
								try {
									Thread.sleep(5000L * 2 ^ i);
								} catch (InterruptedException e) {
								}
							}
						}

						log.error("Could not return build result to: {}",
							buildRequest.getCallbackUrl());
					}

					private Builder prepareCallback(Client client) {
						String userPass = config.getClientId() + ":"
							+ config.getClientSecret();
						String authorization = "Basic "
							+ Base64.encodeBytes(userPass.getBytes());
						return client.target(buildRequest.getCallbackUrl())
							.request().header("Authorization", authorization);
					}

					@Override
					public void onFailure(Throwable t) {
						// TODO Auto-generated method stub

					}

				}, executor);
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		if (build == null) {
			return Response.status(Status.CONFLICT)
					.entity("Server cannot accept build request.").build();
		}

		return Response.ok(build.getUUID()).build();
	}

	@DELETE
	@RequireAuthentication
	@Path("{buildId}")
	public void killBuild(@PathParam("buildId") UUID buildId) {
		manager.killBuild(buildId);
	}

}
