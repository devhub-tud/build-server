package nl.tudelft.ewi.build.jaxrs;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.io.OutputStream;

import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.docker.DockerManager;
import nl.tudelft.ewi.build.docker.ImageBuildObserver;
import nl.tudelft.ewi.build.jaxrs.filters.RequireAuthentication;
import nl.tudelft.ewi.build.jaxrs.models.ImageRequest;

@Slf4j
@Path("api/images")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImagesResource {

	private final DockerManager manager;

	@Inject
	public ImagesResource(DockerManager manager) {
		this.manager = manager;
	}

	@POST
	@RequireAuthentication
	public StreamingOutput onImageRequest(final @Valid ImageRequest imageRequest) {
		return new StreamingOutput() {
			@Override
			public void write(final OutputStream output) throws IOException, WebApplicationException {
				manager.buildImage(imageRequest.getName(), imageRequest.getInstructions(), new ImageBuildObserver() {
					@Override
					public void onMessage(String message) {
						try {
							if (!message.endsWith("\n")) {
								message += "\n";
							}
							output.write(message.getBytes());
							output.flush();
						} 
						catch (IOException e) {
							log.error(e.getMessage(), e);
						}
					}
					
					@Override
					public void onError(String message) {
						try {
							if (message.endsWith("\n")) {
								message = message.substring(0, message.length() - 1);
							}
							output.write(message.getBytes());
							output.flush();
						} 
						catch (IOException e) {
							log.error(e.getMessage(), e);
						}
					}
					
					@Override
					public void onCompleted() {
						try {
							output.close();
						} 
						catch (IOException e) {
							log.error(e.getMessage(), e);
						}
					}
				});
			}
		};
	}

}
