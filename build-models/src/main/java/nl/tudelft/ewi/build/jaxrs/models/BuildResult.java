package nl.tudelft.ewi.build.jaxrs.models;

import java.util.List;

import lombok.Data;

@Data
public class BuildResult {
	
	public static enum Status {
		SUCCEEDED, FAILED;
	}
	
	private Status status;
	private List<String> logLines;
	
}
