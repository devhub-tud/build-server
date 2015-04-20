package nl.tudelft.ewi.build.extensions.instructions;

import nl.tudelft.ewi.build.jaxrs.models.BuildInstruction;

public interface BuildInstructionInterpreter<T extends BuildInstruction> {
	
	String getImage(T instruction);
	
	String getCommand(T instruction);

}
