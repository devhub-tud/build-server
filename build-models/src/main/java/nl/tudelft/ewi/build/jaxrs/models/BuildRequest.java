package nl.tudelft.ewi.build.jaxrs.models;

import lombok.Data;

@Data
public class BuildRequest {

	private Source source;
	
	private BuildInstruction instruction;
	
	private String callbackUrl;
	
}
