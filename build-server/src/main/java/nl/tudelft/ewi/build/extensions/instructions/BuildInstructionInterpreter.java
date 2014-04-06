package nl.tudelft.ewi.build.extensions.instructions;

import java.io.File;

import nl.tudelft.ewi.build.docker.DefaultLogger;
import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult;

public interface BuildInstructionInterpreter<T extends BuildInstruction> {
	
	String getImage(T instruction);
	
	String getCommand(T instruction);

	BuildResult createResult(T instruction, DefaultLogger logger, File stagingDirectory);
	
}
