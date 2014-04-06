package nl.tudelft.ewi.build.docker;

import java.util.Map;

import lombok.Data;

@Data
public class DockerJob {

	private String command;
	private String image;
	private String workingDirectory;
	private Map<String, String> mounts;
	
}
