package nl.tudelft.ewi.build.jaxrs.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MavenBuildInstruction extends BuildInstruction {

	private boolean withDisplay;
	
	private String[] phases;
	
}
