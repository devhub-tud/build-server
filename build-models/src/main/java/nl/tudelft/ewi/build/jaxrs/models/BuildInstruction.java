package nl.tudelft.ewi.build.jaxrs.models;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class BuildInstruction {
	
	public static enum Type {
		MAVEN;
	}
	
	@Setter(AccessLevel.PROTECTED)
	private Type type;
	
}
