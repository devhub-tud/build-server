package nl.tudelft.ewi.build.jaxrs.models;

import lombok.Data;

@Data
public abstract class Source {
	
	public static enum Type {
		GIT;
	}
	
	private Type type;

}
