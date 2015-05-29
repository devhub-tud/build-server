package nl.tudelft.ewi.build.jaxrs.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.tudelft.ewi.build.jaxrs.models.plugins.MavenBuildPlugin;

@Data
@EqualsAndHashCode(callSuper = true)
public class MavenBuildInstruction extends BuildInstruction<MavenBuildPlugin> {
	
	public MavenBuildInstruction() {
		setType(Type.MAVEN);
	}

	private boolean withDisplay;
	
	private String[] phases;
	
}
