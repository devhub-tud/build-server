package nl.tudelft.ewi.build.jaxrs.models;

import lombok.Data;

@Data
public class BuildInstruction {
	
	public static enum Type {
		MAVEN;
	}
	
	private Type type;
	
}
