package nl.tudelft.ewi.build.extensions.instructions;

import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;

import java.io.File;


public interface BuildInstructionInterpreter<T extends BuildInstruction> {

	void runPluginBefores(T instruction, File stagingDirectory);

	void runPluginAfters(T instruction, File stagingDirectory);

	String getImage(T instruction);
	
	String getCommand(T instruction);

}
