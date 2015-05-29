package nl.tudelft.ewi.build.jaxrs.models;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import nl.tudelft.ewi.build.jaxrs.models.plugins.BuildPlugin;

import java.util.List;

@Data
public abstract class BuildInstruction<T extends BuildPlugin> {
	
	public static enum Type {
		MAVEN;
	}
	
	@Setter(AccessLevel.PROTECTED)
	private Type type;

	private List<T> plugins;
	
}
