package nl.tudelft.ewi.build;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SimpleConfig implements Config {

	private int httpPort = 8081;
	private int maximumConcurrentJobs = 1;
	private String dockerHost = "http://localhost:4243/";
	private String stagingDirectory = "workshop";
	private String workingDirectory = "/workshop";
	private String clientId = "test-client";
	private String clientSecret = "test-secret";
	private String certificateDirectory = "/certs";

}
