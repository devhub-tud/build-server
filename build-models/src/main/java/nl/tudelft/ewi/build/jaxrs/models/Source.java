package nl.tudelft.ewi.build.jaxrs.models;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public abstract class Source {
	
	public static enum Type {
		GIT;
	}
	
	@Setter(AccessLevel.PROTECTED)
	private Type type;

}
